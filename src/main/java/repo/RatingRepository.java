package repo;

import db.Db;
import model.Rating;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class RatingRepository {

    private Rating map(ResultSet rs) throws SQLException {
        String id = rs.getObject("id").toString();
        String userId = rs.getObject("user_id").toString();
        String placeId = rs.getObject("place_id").toString();
        int score = rs.getInt("score");
        String description = rs.getString("description");
        Array arr = rs.getArray("hashtags");
        Set<String> tags = new HashSet<>();
        if (arr != null) Collections.addAll(tags, (String[]) arr.getArray());
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();
        return new Rating(userId, placeId, score, description, tags, createdAt);
    }

    public Rating save(Rating r) {
        String sql = "INSERT INTO place_ratings(id, user_id, place_id, score, description, hashtags, created_at) " +
                     "VALUES (?,?,?,?,?,?,?)";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(r.getId()));
            ps.setObject(2, java.util.UUID.fromString(r.getUserId()));
            ps.setObject(3, java.util.UUID.fromString(r.getPlaceId()));
            ps.setInt(4, r.getScore());
            ps.setString(5, r.getDescription());
            ps.setArray(6, c.createArrayOf("text", r.getHashtags().toArray(new String[0])));
            ps.setTimestamp(7, Timestamp.valueOf(r.getCreatedAt()));
            ps.executeUpdate();
            return r;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<Rating> findByUser(String userId) {
        String sql = "SELECT id, user_id, place_id, score, description, hashtags, created_at " +
                     "FROM place_ratings WHERE user_id=? ORDER BY created_at DESC";
        List<Rating> out = new ArrayList<>();
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(userId));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(map(rs)); }
            return out;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public boolean existsByUserAndPlace(String userId, String placeId) {
        String sql = "SELECT 1 FROM place_ratings WHERE user_id=? AND place_id=? LIMIT 1";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(userId));
            ps.setObject(2, java.util.UUID.fromString(placeId));
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public boolean userHasPositiveRatingForType(String userId, String typeTag) {
        // pozitivan (>=4) za neko mesto koje sadrži dati "tip" hešteg (npr. #bioskop)
        String sql =
            "SELECT 1 " +
            "FROM place_ratings r " +
            "JOIN places p ON p.id = r.place_id " +
            "WHERE r.user_id=? AND r.score >= 4 AND ? = ANY(p.hashtags) " +
            "LIMIT 1";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(userId));
            ps.setString(2, typeTag);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }
}
