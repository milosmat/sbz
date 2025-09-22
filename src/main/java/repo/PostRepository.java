package repo;

import model.Post;
import db.Db;
import util.Hydrator;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class PostRepository {

    public Post save(Post p) {
        String sql = "INSERT INTO posts(id, author_id, text_body, hashtags, likes, reports, created_at) " +
                     "VALUES (?,?,?,?,?,?,?) " +
                     "ON CONFLICT (id) DO UPDATE SET text_body=EXCLUDED.text_body, hashtags=EXCLUDED.hashtags"; // menja tekst/htagove
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(p.getId()));
            ps.setObject(2, java.util.UUID.fromString(p.getAuthorId()));
            ps.setString(3, p.getText());
            Array arr = c.createArrayOf("text", p.getHashtags().toArray(new String[0]));
            ps.setArray(4, arr);
            ps.setInt(5, p.getLikes());
            ps.setInt(6, p.getReports());
            ps.setTimestamp(7, Timestamp.valueOf(p.getCreatedAt()));
            ps.executeUpdate();
            return p;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<Post> findByAuthor(String authorId) {
        String sql = "SELECT id, author_id, text_body, hashtags, likes, reports, created_at " +
                     "FROM posts WHERE author_id=? ORDER BY created_at DESC";
        List<Post> out = new ArrayList<>();
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(authorId));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
            return out;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public boolean existsById(String postId) {
        if (postId == null) return false;
        String sql = "SELECT 1 FROM posts WHERE id=? LIMIT 1";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(postId));
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public boolean hasUserLiked(String postId, String userId) {
        String sql = "SELECT 1 FROM post_likes WHERE post_id=? AND user_id=? LIMIT 1";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(postId));
            ps.setObject(2, java.util.UUID.fromString(userId));
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    /** vraća post nakon eventualne izmene broja lajkova */
    public Post like(String postId, String userId) {
        if (!existsById(postId)) throw new IllegalArgumentException("Objava ne postoji.");
        if (userId == null || userId.trim().isEmpty()) throw new IllegalArgumentException("Niste ulogovani.");

        Connection c = null;
        try {
            c = Db.get(); c.setAutoCommit(false);

            int ins;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO post_likes(post_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING")) {
                ps.setObject(1, java.util.UUID.fromString(postId));
                ps.setObject(2, java.util.UUID.fromString(userId));
                ins = ps.executeUpdate();
            }
            if (ins == 1) { // prvi put lajkuje
                try (PreparedStatement ps = c.prepareStatement("UPDATE posts SET likes = likes + 1 WHERE id=?")) {
                    ps.setObject(1, java.util.UUID.fromString(postId));
                    ps.executeUpdate();
                }
            }

            c.commit();
        } catch (SQLException e) {
            try { if (c != null) c.rollback(); } catch (SQLException ignore) {}
            throw new RuntimeException(e);
        } finally {
            try { if (c != null) c.setAutoCommit(true); } catch (SQLException ignore) {}
        }
        return findById(postId).orElseThrow(() -> new IllegalStateException("Post nestao posle lajka?"));
    }

    public Optional<Post> findById(String id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT id, author_id, text_body, hashtags, likes, reports, created_at FROM posts WHERE id=?";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(id));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(map(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public boolean hasUserReported(String postId, String userId) {
        String sql = "SELECT 1 FROM post_reports WHERE post_id=? AND user_id=? LIMIT 1";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(postId));
            ps.setObject(2, java.util.UUID.fromString(userId));
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public Post report(String postId, String userId) {
        if (!existsById(postId)) throw new IllegalArgumentException("Objava ne postoji.");
        if (userId == null || userId.trim().isEmpty()) throw new IllegalArgumentException("Niste ulogovani.");

        Connection c = null;
        try {
            c = Db.get(); c.setAutoCommit(false);

            int ins;
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO post_reports(post_id, user_id) VALUES (?, ?) ON CONFLICT DO NOTHING")) {
                ps.setObject(1, java.util.UUID.fromString(postId));
                ps.setObject(2, java.util.UUID.fromString(userId));
                ins = ps.executeUpdate();
            }
            if (ins == 1) {
                try (PreparedStatement ps = c.prepareStatement("UPDATE posts SET reports = reports + 1 WHERE id=?")) {
                    ps.setObject(1, java.util.UUID.fromString(postId));
                    ps.executeUpdate();
                }
            }

            c.commit();
        } catch (SQLException e) {
            try { if (c != null) c.rollback(); } catch (SQLException ignore) {}
            throw new RuntimeException(e);
        } finally {
            try { if (c != null) c.setAutoCommit(true); } catch (SQLException ignore) {}
        }
        return findById(postId).orElseThrow(() -> new IllegalStateException("Post nestao posle prijave?"));
    }

    // --- mapper ---
    private Post map(ResultSet rs) throws SQLException {
        String id   = rs.getObject("id", java.util.UUID.class).toString();
        String auth = rs.getObject("author_id", java.util.UUID.class).toString();
        String text = rs.getString("text_body");
        java.sql.Array a = rs.getArray("hashtags");
        java.util.Set<String> tags = new java.util.HashSet<>();
        if (a != null) tags.addAll(java.util.Arrays.asList((String[]) a.getArray()));
        int likes   = rs.getInt("likes");
        int reports = rs.getInt("reports");
        java.time.LocalDateTime created = rs.getTimestamp("created_at").toLocalDateTime();

        Post p = new Post(auth, text, tags); // generiše novi id/createdAt → hidriraćemo
        util.Hydrator.set(p, "id", id);
        util.Hydrator.set(p, "createdAt", created);
        p.setLikes(likes);
        p.setReports(reports);
        return p;
    }
}
