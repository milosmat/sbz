package http;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dto.CreatePostRequest;
import model.Post;
import repo.FriendRepository;
import repo.PostRepository;
import repo.UserRepository;
import service.AuthService;
import service.FeedService;
import service.FriendService;
import service.PostService;
import service.RegistrationService;
import repo.PlaceRepository;
import service.PlaceService;
import dto.CreatePlaceRequest;
import model.Place;
import repo.ModerationEventsRepository;
import repo.ModerationEventsRepository.Flagged;
import service.ModerationService;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Minimalni JSON API server (JDK 1.8, com.sun.net.httpserver)
 * Endpoints:
 *  GET  /api/health
 *  GET  /api/feed/friends?userId=...&days=1&page=0&size=20
 *  GET  /api/posts/by-author?authorId=...
 *  POST /api/posts/like?postId=...&userId=...
 *  POST /api/posts/report?postId=...&userId=...&reason=...
 *  POST /api/posts (body: { authorId, text, tags })
 */
public class ApiServer {

    private static final Gson GSON = new Gson();

    // Repozitorijumi i servisi (JDBC)
    private final UserRepository userRepo = new UserRepository();
    private final PostRepository postRepo = new PostRepository();
    private final FriendRepository friendRepo = new FriendRepository();
    private final PlaceRepository placeRepo = new PlaceRepository();
    private final ModerationEventsRepository modRepo = ModerationEventsRepository.getInstance();
    
    private final ModerationService moderationService = new ModerationService(userRepo, modRepo);
    private final FriendService friendService = new FriendService(userRepo, friendRepo);
    private final PostService postService     = new PostService(postRepo, userRepo);
    private final AuthService authService     = new AuthService(userRepo); // ako ti zatreba kasnije
    private final RegistrationService regService = new RegistrationService(userRepo);
    private final PlaceService placeService = new PlaceService(placeRepo, userRepo);
    
    private final FeedService feedService = new service.FeedService(userRepo, friendRepo, postRepo);
    
    private final SessionManager sessionManager = new SessionManager();
    
    private final Object modLock = new Object();
    private volatile long lastModRun = 0L;

    
    public static void main(String[] args) throws Exception {
        new ApiServer().start(8080);
    }

    public void start(int port) throws Exception {
        HttpServer s = HttpServer.create(new InetSocketAddress(port), 0);
        s.setExecutor(Executors.newFixedThreadPool(8));
        
        s.createContext("/api/auth/login", Cors.wrap(ex -> {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { Cors.handlePreflight(ex); return; }
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { methodNotAllowed(ex); return; }

            String body = readBody(ex.getRequestBody());
            dto.LoginRequest req = GSON.fromJson(body, dto.LoginRequest.class);
            if (req == null || req.email == null || req.password == null) {
                badRequest(ex, "email and password required"); return;
            }

            try {
                model.User u = authService.login(new dto.LoginRequest(req.email, req.password));

                // napravi token i zapamti ga
                String token = sessionManager.createSession(u.getId());

                Map<String,Object> out = new LinkedHashMap<>();
                out.put("token", token);

                Map<String,Object> userDto = new LinkedHashMap<>();
                userDto.put("id", u.getId());
                userDto.put("firstName", u.getFirstName());
                userDto.put("lastName", u.getLastName());
                userDto.put("email", u.getEmail());
                userDto.put("city", u.getCity());
                out.put("user", userDto);

                ok(ex, out);
            } catch (IllegalArgumentException iae) {
                badRequest(ex, iae.getMessage());
            }
        }));
        
        s.createContext("/api/auth/register", Cors.wrap(ex -> {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { Cors.handlePreflight(ex); return; }
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { methodNotAllowed(ex); return; }

            try {
                String body = readBody(ex.getRequestBody());
                dto.RegisterRequest req = GSON.fromJson(body, dto.RegisterRequest.class);
                if (req == null || req.firstName == null || req.lastName == null
                        || req.email == null || req.password == null || req.city == null) {
                    badRequest(ex, "firstName, lastName, email, password, city are required");
                    return;
                }

                model.User u = regService.register(req);

                // vrati User DTO (isti oblik koji front očekuje iz AuthService.register)
                Map<String,Object> userDto = new LinkedHashMap<>();
                userDto.put("id", u.getId());
                userDto.put("firstName", u.getFirstName());
                userDto.put("lastName", u.getLastName());
                userDto.put("email", u.getEmail());
                userDto.put("city", u.getCity());

                ok(ex, userDto);
            } catch (IllegalArgumentException iae) {
                // npr. email zauzet ili validacija iz pravila
                badRequest(ex, iae.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                byte[] b = GSON.toJson(map("error","internal error","detail", String.valueOf(e.getMessage())))
                               .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                ex.sendResponseHeaders(500, b.length);
                try (java.io.OutputStream os = ex.getResponseBody()) { os.write(b); }
                ex.close();
            }
        }));

        s.createContext("/api/posts/by/", Cors.wrap(ex -> {
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) { methodNotAllowed(ex); return; }
            String path = ex.getRequestURI().getPath(); // npr. /api/posts/by/UUID
            String prefix = "/api/posts/by/";
            if (!path.startsWith(prefix) || path.length() <= prefix.length()) { badRequest(ex, "userId required"); return; }
            String userId = path.substring(prefix.length());
            List<PostDTO> list = postRepo.findByAuthor(userId).stream().map(ApiServer::toDto).collect(Collectors.toList());
            ok(ex, list);
        }));
        
        // feed prijatelja (1 dan po difoltu)
        s.createContext("/api/feed/friends", Cors.wrap(ex -> {
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) { methodNotAllowed(ex); return; }
            Map<String,List<String>> q = Query.params(ex);
            String userId = Query.str(q, "userId", null);
            int days = (int) Query.num(q, "days", 1);
            int page = (int) Query.num(q, "page", 0);
            int size = (int) Query.num(q, "size", 20);
            if (userId == null) { badRequest(ex, "userId is required"); return; }

            // ID-jevi prijatelja
            Set<String> friendIds = friendRepo.getFriendsOf(userId);

            // Postovi prijatelja (sortirani u repo-u po autoru; ovde još jednom globalno sort)
            List<Post> all = new ArrayList<>();
            for (String fid : friendIds) {
                all.addAll(postRepo.findByAuthor(fid));
            }
            long cutoffMs = System.currentTimeMillis() - days * 24L * 3600_000L;
            List<Post> filtered = all.stream()
                    .filter(p -> p.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli() >= cutoffMs)
                    .sorted((a,b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                    .collect(Collectors.toList());

            // stranica
            int from = Math.max(0, page*size);
            int to = Math.min(filtered.size(), from+size);
            List<Post> slice = from >= to ? Collections.emptyList() : filtered.subList(from, to);

            Page<PostDTO> out = new Page<>();
            out.page = page; out.size = size; out.totalElements = filtered.size();
            out.content = slice.stream().map(ApiServer::toDto).collect(Collectors.toList());
            ok(ex, out);
        }));

        // postovi po autoru
        s.createContext("/api/posts/by-author", Cors.wrap(ex -> {
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) { methodNotAllowed(ex); return; }
            Map<String,List<String>> q = Query.params(ex);
            String authorId = Query.str(q, "authorId", null);
            if (authorId == null) { badRequest(ex, "authorId is required"); return; }
            List<PostDTO> list = postRepo.findByAuthor(authorId).stream().map(ApiServer::toDto).collect(Collectors.toList());
            ok(ex, list);
        }));

        s.createContext("/api/posts/", Cors.wrap(ex -> {
            try {
                if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { Cors.handlePreflight(ex); return; }

                String path = ex.getRequestURI().getPath();
                final String base = "/api/posts/";
                if (!path.startsWith(base) || path.length() <= base.length()) { methodNotAllowed(ex); return; }
                String rest = path.substring(base.length()); // "123/like" ili "123/report"

                if (rest.endsWith("/like")) {
                    if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { methodNotAllowed(ex); return; }
                    String postId = rest.substring(0, rest.length() - "/like".length());


                    Optional<String> uid = requireAuth(ex);
                    if (!uid.isPresent()) return;

                    System.out.println("[LIKE] postId="+postId+" userId="+uid.get());
                    Post p = postService.likePost(uid.get(), postId);
                    System.out.println("[LIKE] done -> likes="+p.getLikes());
                    ok(ex, toDto(p));
                    return;
                }

                if (rest.endsWith("/report")) {
                    if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { methodNotAllowed(ex); return; }
                    String postId = rest.substring(0, rest.length() - "/report".length());

                    Optional<String> uid = requireAuth(ex);
                    if (!uid.isPresent()) return;

                    String reason = "";
                    try {
                        String body = readBody(ex.getRequestBody());
                        java.util.Map<?,?> parsed = GSON.fromJson(body, java.util.Map.class);
                        if (parsed != null && parsed.get("reason") != null) reason = String.valueOf(parsed.get("reason"));
                    } catch (Exception ignore) {}

                    System.out.println("[REPORT] postId="+postId+" userId="+uid.get()+" reason="+reason);
                    Post p = postService.reportPost(uid.get(), postId, reason);
                    System.out.println("[REPORT] done -> reports="+p.getReports());
                    maybeKickModerationAsync();
                    ok(ex, toDto(p));
                    return;
                }

                // nije pogodilo uzorak
                byte[] body = "{\"error\":\"not found\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
                ex.sendResponseHeaders(404, body.length);
                try (java.io.OutputStream os = ex.getResponseBody()) { os.write(body); }
                ex.close();
            } catch (Exception e) {
                e.printStackTrace();
                // sigurni 500 sa CORS headerima
                byte[] b = GSON.toJson(map("error","internal error","detail", String.valueOf(e.getMessage()))).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                ex.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
                ex.sendResponseHeaders(500, b.length);
                try (java.io.OutputStream os = ex.getResponseBody()) { os.write(b); }
                ex.close();
            }
        }));
        
        s.createContext("/api/friends/search", Cors.wrap(ex -> {
            try {
                if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) { methodNotAllowed(ex); return; }
                Optional<String> uid = requireAuth(ex);
                if (!uid.isPresent()) return;

                Map<String, List<String>> q = Query.params(ex);
                String term = Query.str(q, "q", "");
                int limit = (int) Query.num(q, "limit", 20);

                java.util.List<model.User> found = friendService.searchUsers(uid.get(), term, limit);

                // mapiraj User -> DTO koji front očekuje
                java.util.List<java.util.Map<String,Object>> out = new java.util.ArrayList<>();
                for (model.User u : found) {
                    java.util.Map<String,Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", u.getId());
                    m.put("firstName", u.getFirstName());
                    m.put("lastName", u.getLastName());
                    m.put("email", u.getEmail());
                    m.put("city", u.getCity());
                    out.add(m);
                }
                ok(ex, out);
            } catch (Exception e) {
                e.printStackTrace();
                byte[] b = GSON.toJson(map("error","internal error","detail", String.valueOf(e.getMessage()))).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                ex.getResponseHeaders().set("Content-Type","application/json; charset=UTF-8");
                ex.sendResponseHeaders(500, b.length);
                try (java.io.OutputStream os = ex.getResponseBody()) { os.write(b); }
                ex.close();
            }
        }));

        // POST /api/friends   body: { friendId }
        s.createContext("/api/friends", Cors.wrap(ex -> {
            try {
                if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { Cors.handlePreflight(ex); return; }
                if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { methodNotAllowed(ex); return; }

                Optional<String> uid = requireAuth(ex);
                if (!uid.isPresent()) return;

                String body = readBody(ex.getRequestBody());
                java.util.Map<?,?> json = GSON.fromJson(body, java.util.Map.class);
                String friendId = json == null ? null : (json.get("friendId") == null ? null : String.valueOf(json.get("friendId")));
                if (friendId == null || friendId.trim().isEmpty()) { badRequest(ex, "friendId is required"); return; }

                friendService.addFriend(uid.get(), friendId.trim());
                ok(ex, map("status","ok"));
            } catch (IllegalArgumentException iae) {
                badRequest(ex, iae.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                byte[] b = GSON.toJson(map("error","internal error","detail", String.valueOf(e.getMessage()))).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                ex.getResponseHeaders().set("Content-Type","application/json; charset=UTF-8");
                ex.sendResponseHeaders(500, b.length);
                try (java.io.OutputStream os = ex.getResponseBody()) { os.write(b); }
                ex.close();
            }
        }));

        // POST /api/friends/block   body: { friendId }
        s.createContext("/api/friends/block", Cors.wrap(ex -> {
            try {
                if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { Cors.handlePreflight(ex); return; }
                if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { methodNotAllowed(ex); return; }

                Optional<String> uid = requireAuth(ex);
                if (!uid.isPresent()) return;

                String body = readBody(ex.getRequestBody());
                java.util.Map<?,?> json = GSON.fromJson(body, java.util.Map.class);
                String friendId = json == null ? null : (json.get("friendId") == null ? null : String.valueOf(json.get("friendId")));
                if (friendId == null || friendId.trim().isEmpty()) { badRequest(ex, "friendId is required"); return; }

                friendService.blockUser(uid.get(), friendId.trim());
                maybeKickModerationAsync();
                ok(ex, map("status","ok"));
            } catch (IllegalArgumentException iae) {
                badRequest(ex, iae.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                byte[] b = GSON.toJson(map("error","internal error","detail", String.valueOf(e.getMessage()))).getBytes(java.nio.charset.StandardCharsets.UTF_8);
                ex.getResponseHeaders().set("Content-Type","application/json; charset=UTF-8");
                ex.sendResponseHeaders(500, b.length);
                try (java.io.OutputStream os = ex.getResponseBody()) { os.write(b); }
                ex.close();
            }
        }));

        s.createContext("/api/places", Cors.wrap(ex -> {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { Cors.handlePreflight(ex); return; }
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { methodNotAllowed(ex); return; }

            Optional<String> uid = requireAuth(ex);
            if (!uid.isPresent()) return;

            try {
                String body = readBody(ex.getRequestBody());
                Map<?,?> json = GSON.fromJson(body, Map.class);

                CreatePlaceRequest req = new CreatePlaceRequest(
                    uid.get(),
                    str(json,"name"),
                    str(json,"country"),
                    str(json,"city"),
                    str(json,"description"),
                    str(json,"hashtagsLine")
                );

                Place p = placeService.createPlace(req);

                Map<String,Object> out = new LinkedHashMap<>();
                out.put("id", p.getId());
                out.put("name", p.getName());
                out.put("country", p.getCountry());
                out.put("city", p.getCity());
                out.put("description", p.getDescription());
                out.put("hashtags", new java.util.ArrayList<>(p.getHashtags()));
                ok(ex, out);
            } catch (IllegalArgumentException iae) {
                badRequest(ex, iae.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                byte[] b = GSON.toJson(map("error","internal error","detail", String.valueOf(e.getMessage())))
                               .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                ex.getResponseHeaders().set("Content-Type","application/json; charset=UTF-8");
                ex.sendResponseHeaders(500, b.length);
                try (java.io.OutputStream os = ex.getResponseBody()) { os.write(b); }
                ex.close();
            }
        }));

        // kreiraj post (JSON body)
        s.createContext("/api/posts", Cors.wrap(ex -> {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { Cors.handlePreflight(ex); return; }
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { methodNotAllowed(ex); return; }
            String body = readBody(ex.getRequestBody());
            CreatePostRequest req = GSON.fromJson(body, CreatePostRequest.class);
            if (req == null || req.authorId == null || req.text == null) {
                badRequest(ex, "Invalid payload (authorId, text required)"); return;
            }
            try {
                Post p = postService.createPost(req);
                ok(ex, toDto(p));
            } catch (IllegalArgumentException iae) {
                badRequest(ex, iae.getMessage());
            }
        }));
        
        s.createContext("/api/admin/mod/flags", Cors.wrap(ex -> {
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) { methodNotAllowed(ex); return; }

            // provera da je admin
            Optional<String> uid = requireAuth(ex);
            if (!uid.isPresent()) return;
            if (!userRepo.isAdmin(uid.get())) { badRequest(ex, "admin only"); return; }

            Map<String,List<String>> q = Query.params(ex);
            int hours = (int) Query.num(q, "sinceHours", 168); // 7 dana default
            int limit = (int) Query.num(q, "limit", 200);

            long since = System.currentTimeMillis() - hours * 3600_000L;

            List<Flagged> list = modRepo.listRecentFlags(since, limit);

            // obogatimo sa user info (ime/prezime)
            List<Map<String,Object>> out = new ArrayList<Map<String,Object>>();
            for (Flagged f : list) {
                Optional<model.User> uOpt = userRepo.findById(f.userId);
                Map<String,Object> m = new LinkedHashMap<String,Object>();
                m.put("userId", f.userId);
                m.put("reason", f.reason);
                m.put("untilMs", f.until);
                if (uOpt.isPresent()) {
                    model.User u = uOpt.get();
                    m.put("firstName", u.getFirstName());
                    m.put("lastName",  u.getLastName());
                    m.put("email",     u.getEmail());
                }
                out.add(m);
            }
            ok(ex, out);
        }));
        
        // feed prijatelja
        s.createContext("/api/feed/friends", Cors.wrap(ex -> {
        	if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) { methodNotAllowed(ex); return; }
        	Optional<String> uid = requireAuth(ex); if (!uid.isPresent()) return;
        	try {
	        	java.time.LocalDateTime now = java.time.LocalDateTime.now();
	        	java.util.List<model.Post> list = feedService.friendFeed(uid.get(), now);
	        	java.util.List<PostDTO> out = list.stream().map(ApiServer::toDto).collect(java.util.stream.Collectors.toList());
	        	ok(ex, out);
        	} catch (IllegalArgumentException iae) { badRequest(ex, iae.getMessage()); }
        }));
        
        // preporuceni feed
        s.createContext("/api/feed/recommended", Cors.wrap(ex -> {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { Cors.handlePreflight(ex); return; }
            if (!"GET".equalsIgnoreCase(ex.getRequestMethod())) { methodNotAllowed(ex); return; }

            Map<String,List<String>> q = Query.params(ex);
            String userId = Query.str(q, "userId", null);
            int limit = (int) Query.num(q, "limit", 20);

            if (userId == null || userId.trim().isEmpty()) {
                badRequest(ex, "userId is required");
                return;
            }

            try {
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                java.util.List<dto.CandidatePost> recs = feedService.recommendedFeed(userId, now, limit);

                // DTO za izlaz (bez nullova)
                class RecDTO { PostDTO post; int score; java.util.List<String> reasons; }
                java.util.List<RecDTO> out = new java.util.ArrayList<>();
                if (recs != null) {
                    for (dto.CandidatePost c : recs) {
                        if (c == null || c.getPost() == null) continue;
                        RecDTO d = new RecDTO();
                        d.post = toDto(c.getPost());
                        d.score = c.getScore();
                        d.reasons = c.getReasons() == null ? java.util.Collections.emptyList() : c.getReasons();
                        out.add(d);
                    }
                }

                ok(ex, out);
            } catch (IllegalArgumentException iae) {
                badRequest(ex, iae.getMessage());
            } catch (Exception e) {
                e.printStackTrace();
                badRequest(ex, "internal error");
            }
        }));
        
        
        s.createContext("/api/", Cors.wrap(ex -> {
            if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) { Cors.handlePreflight(ex); return; }
            byte[] body = "{\"error\":\"not found\"}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            ex.sendResponseHeaders(404, body.length);
            try (java.io.OutputStream os = ex.getResponseBody()) { os.write(body); }
            ex.close();
        }));

        s.start();
        System.out.println("[HTTP] listening on http://localhost:" + port);
    }

    private static String readBody(java.io.InputStream in) throws java.io.IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        int r;
        while ((r = in.read(buf)) != -1) bos.write(buf, 0, r);
        return new String(bos.toByteArray(), java.nio.charset.StandardCharsets.UTF_8);
    }
    
    private Optional<String> requireAuth(HttpExchange ex) throws java.io.IOException {
        String authHeader = ex.getRequestHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            badRequest(ex, "Missing or invalid Authorization header");
            return Optional.empty();
        }
        String token = authHeader.substring("Bearer ".length()).trim();
        return sessionManager.getUserId(token);
    }
    
    private static String str(Map<?,?> m, String k){
        Object v = (m==null)? null : m.get(k);
        return v==null ? null : String.valueOf(v);
    }
    
    private void maybeKickModerationAsync() {
        long now = System.currentTimeMillis();
        if (now - lastModRun < 30_000) return;
        synchronized (modLock) {
            if (now - lastModRun < 30_000) return;
            lastModRun = now;
        }
        new Thread(() -> {
            try {
                List<Flagged> flagged = moderationService.detectAndSuspend();
                for (Flagged f : flagged) {
                    try { userRepo.setPostingSuspendedUntil(f.userId, f.until); } catch (Exception ignore) {}
                }
            } catch (Exception e) { e.printStackTrace(); }
        }, "moderation-kick").start();
    }
    
    /* ==== DTOs ==== */
    static class Page<T> { public List<T> content; public int page; public int size; public long totalElements; }

    static class PostDTO {
        public String id;
        public String authorId;
        public String text;
        public List<String> hashtags;
        public int likes;
        public int reports;
        public long createdAtEpochMs;
    }

    private static PostDTO toDto(Post p){
        PostDTO d = new PostDTO();
        d.id = p.getId();
        d.authorId = p.getAuthorId();
        d.text = p.getText();
        d.hashtags = new ArrayList<>(p.getHashtags());
        d.likes = p.getLikes();
        d.reports = p.getReports();
        d.createdAtEpochMs = p.getCreatedAt().toInstant(ZoneOffset.UTC).toEpochMilli();
        return d;
    }

    /* ==== Helpers ==== */

    private static void ok(HttpExchange ex, Object payload) throws java.io.IOException { send(ex, 200, payload); }
    private static void badRequest(HttpExchange ex, String msg) throws java.io.IOException { send(ex, 400, map("error", msg)); }
    private static void methodNotAllowed(HttpExchange ex) throws java.io.IOException { send(ex, 405, map("error","method not allowed")); }

    private static void send(HttpExchange ex, int status, Object payload) throws java.io.IOException {
        // CORS headeri se već postavljaju u Cors.wrap() -> ovde samo body
        byte[] body = payload == null ? new byte[0] : GSON.toJson(payload).getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(body); }
        ex.close();
    }
    private static Map<String,Object> map(Object... kv){
        Map<String,Object> m = new LinkedHashMap<>();
        for (int i=0;i+1<kv.length;i+=2) m.put(String.valueOf(kv[i]), kv[i+1]);
        return m;
    }

    static class Query {
        static Map<String, List<String>> params(HttpExchange ex){
            String raw = ex.getRequestURI().getRawQuery();
            Map<String, List<String>> map = new HashMap<>();
            if (raw == null || raw.isEmpty()) return map;
            for (String pair : raw.split("&")) {
                int i = pair.indexOf('=');
                String k = i>0 ? decode(pair.substring(0,i)) : decode(pair);
                String v = i>0 && pair.length()>i+1 ? decode(pair.substring(i+1)) : "";
                map.computeIfAbsent(k, __-> new ArrayList<>()).add(v);
            }
            return map;
        }
        static String decode(String s){ try { return java.net.URLDecoder.decode(s, "UTF-8"); } catch (Exception e) { return s; } }
        static String str(Map<String,List<String>> q, String k, String def){ return q.getOrDefault(k, Collections.emptyList()).stream().findFirst().orElse(def); }
        static long num(Map<String,List<String>> q, String k, long def){ try { return Long.parseLong(str(q,k,String.valueOf(def))); } catch (Exception e){ return def; } }
    }
}
