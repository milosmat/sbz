package repo;

import db.Db;

import java.sql.*;
import java.util.*;

public class FriendRepository {

    private static String lo(String a, String b){ return a.compareTo(b) <= 0 ? a : b; }
    private static String hi(String a, String b){ return a.compareTo(b) <= 0 ? b : a; }

    public boolean areFriends(String a, String b) {
        if (a == null || b == null) return false;
        String sql = "SELECT 1 FROM friendships WHERE user_lo=? AND user_hi=? LIMIT 1";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(lo(a,b)));
            ps.setObject(2, java.util.UUID.fromString(hi(a,b)));
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void addFriends(String a, String b) {
        if (a == null || b == null) return;
        String sql = "INSERT INTO friendships(user_lo, user_hi) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(lo(a,b)));
            ps.setObject(2, java.util.UUID.fromString(hi(a,b)));
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void removeFriends(String a, String b) {
        if (a == null || b == null) return;
        String sql = "DELETE FROM friendships WHERE user_lo=? AND user_hi=?";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(lo(a,b)));
            ps.setObject(2, java.util.UUID.fromString(hi(a,b)));
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public Set<String> getFriendsOf(String userId) {
        String sql =
            "SELECT CASE WHEN user_lo = ?::uuid THEN user_hi ELSE user_lo END AS friend_id " +
            "FROM friendships WHERE user_lo = ?::uuid OR user_hi = ?::uuid";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            java.util.UUID u = java.util.UUID.fromString(userId);
            ps.setObject(1, u); ps.setObject(2, u); ps.setObject(3, u);
            Set<String> out = new HashSet<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getObject("friend_id", java.util.UUID.class).toString());
            }
            return out;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public boolean isBlocked(String blockerId, String targetId) {
        if (blockerId == null || targetId == null) return false;
        String sql = "SELECT 1 FROM blocks WHERE blocker_id=? AND target_id=? LIMIT 1";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(blockerId));
            ps.setObject(2, java.util.UUID.fromString(targetId));
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void block(String blockerId, String targetId) {
        if (blockerId == null || targetId == null) return;
        Connection c = null;
        try {
            c = Db.get(); c.setAutoCommit(false);

            // 1) ukloni eventualno prijateljstvo
            try (PreparedStatement ps = c.prepareStatement(
                    "DELETE FROM friendships WHERE (user_lo=? AND user_hi=?) OR (user_lo=? AND user_hi=?)")) {
                String lo = lo(blockerId, targetId), hi = hi(blockerId, targetId);
                ps.setObject(1, java.util.UUID.fromString(lo));
                ps.setObject(2, java.util.UUID.fromString(hi));
                ps.setObject(3, java.util.UUID.fromString(lo));
                ps.setObject(4, java.util.UUID.fromString(hi));
                ps.executeUpdate();
            }
            // 2) upiši blok
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO blocks(blocker_id, target_id) VALUES (?, ?) ON CONFLICT DO NOTHING")) {
                ps.setObject(1, java.util.UUID.fromString(blockerId));
                ps.setObject(2, java.util.UUID.fromString(targetId));
                ps.executeUpdate();
            }

            c.commit();
        } catch (SQLException e) {
            try { if (c != null) c.rollback(); } catch (SQLException ignore) {}
            throw new RuntimeException(e);
        } finally {
            try { if (c != null) c.setAutoCommit(true); } catch (SQLException ignore) {}
        }
    }

    public void unblock(String blockerId, String targetId) {
        String sql = "DELETE FROM blocks WHERE blocker_id=? AND target_id=?";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(blockerId));
            ps.setObject(2, java.util.UUID.fromString(targetId));
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public Set<String> getBlockedBy(String userId) {
        String sql = "SELECT target_id FROM blocks WHERE blocker_id=?";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(userId));
            Set<String> out = new HashSet<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(rs.getObject(1, java.util.UUID.class).toString());
            }
            return out;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }
}
