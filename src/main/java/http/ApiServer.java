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
import service.FriendService;
import service.PostService;

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

    private final FriendService friendService = new FriendService(userRepo, friendRepo);
    private final PostService postService     = new PostService(postRepo, userRepo);
    private final AuthService authService     = new AuthService(userRepo); // ako ti zatreba kasnije

    public static void main(String[] args) throws Exception {
        new ApiServer().start(8080);
    }

    public void start(int port) throws Exception {
        HttpServer s = HttpServer.create(new InetSocketAddress(port), 0);
        s.setExecutor(Executors.newFixedThreadPool(8));

        // health
        s.createContext("/api/health", Cors.wrap(ex -> ok(ex, map("status", "ok"))));

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

        // like post
        s.createContext("/api/posts/like", Cors.wrap(ex -> {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { methodNotAllowed(ex); return; }
            Map<String,List<String>> q = Query.params(ex);
            String postId = Query.str(q, "postId", null);
            String userId = Query.str(q, "userId", null);
            if (postId == null || userId == null) { badRequest(ex, "postId and userId are required"); return; }
            Post p = postRepo.like(postId, userId);
            ok(ex, toDto(p));
        }));

        // report post
        s.createContext("/api/posts/report", Cors.wrap(ex -> {
            if (!"POST".equalsIgnoreCase(ex.getRequestMethod())) { methodNotAllowed(ex); return; }
            Map<String,List<String>> q = Query.params(ex);
            String postId = Query.str(q, "postId", null);
            String userId = Query.str(q, "userId", null);
            String reason = Query.str(q, "reason", ""); // opciono
            if (postId == null || userId == null) { badRequest(ex, "postId and userId are required"); return; }
            Post p = postService.reportPost(userId, postId, reason); // koristi servis (isti biznis pravila)
            ok(ex, toDto(p));
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
