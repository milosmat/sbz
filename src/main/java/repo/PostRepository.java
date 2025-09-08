package repo;

import model.Post;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class PostRepository {
    private final Map<String, Post> byId = new ConcurrentHashMap<>();
    // indeks po autoru
    private final Map<String, List<Post>> byAuthor = new ConcurrentHashMap<>();

    private final Map<String, Set<String>> likedBy = new ConcurrentHashMap<>();
    
    private final Map<String, Set<String>> reportedBy = new ConcurrentHashMap<>();
    
    public Post save(Post p) {
        byId.put(p.getId(), p);
        byAuthor.computeIfAbsent(p.getAuthorId(), k -> new ArrayList<>()).add(p);
        return p;
    }

    public List<Post> findByAuthor(String authorId) {
        return byAuthor.getOrDefault(authorId, Collections.emptyList())
                .stream()
                .sorted(Comparator.comparing(Post::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }
    
    public boolean existsById(String postId) {
        return postId != null && byId.containsKey(postId);
    }

    public boolean hasUserLiked(String postId, String userId) {
        Set<String> s = likedBy.get(postId);
        return s != null && userId != null && s.contains(userId);
    }

    /** Vraća post nakon eventualne izmene broja lajkova */
    public Post like(String postId, String userId) {
        if (!existsById(postId)) throw new IllegalArgumentException("Objava ne postoji.");
        if (userId == null || userId.trim().isEmpty()) throw new IllegalArgumentException("Niste ulogovani.");

        Set<String> s = likedBy.computeIfAbsent(postId, k -> Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>()));
        if (s.add(userId)) { // prvi put lajkuje
            Post p = byId.get(postId);
            p.setLikes(p.getLikes() + 1);
            return p;
        }
        return byId.get(postId); // već lajkovao – bez promene
    }

    public Optional<Post> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }
    
    public boolean hasUserReported(String postId, String userId) {
        Set<String> s = reportedBy.get(postId);
        return s != null && userId != null && s.contains(userId);
    }

    public Post report(String postId, String userId) {
        if (!existsById(postId)) throw new IllegalArgumentException("Objava ne postoji.");
        if (userId == null || userId.trim().isEmpty()) throw new IllegalArgumentException("Niste ulogovani.");

        Set<String> s = reportedBy.computeIfAbsent(
                postId, k -> Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>()));
        if (s.add(userId)) { // prvi put prijavljuje
            Post p = byId.get(postId);
            p.setReports(p.getReports() + 1);
            return p;
        }
        return byId.get(postId); // već prijavljena od strane ovog korisnika – bez promene
    }
    
}
