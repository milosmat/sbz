package repo;

import model.User;
import db.Db;
import util.Hydrator;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class UserRepository {

    public void suspendPostingHours(String userId, int hours) {
        long until = System.currentTimeMillis() + hours * 3600_000L;
        String sql = "INSERT INTO post_bans(user_id, until_ms) VALUES (?, ?) " +
                     "ON CONFLICT (user_id) DO UPDATE SET until_ms = GREATEST(post_bans.until_ms, EXCLUDED.until_ms)";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(userId));
            ps.setLong(2, until);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void suspendLoginHours(String userId, int hours) {
        long until = System.currentTimeMillis() + hours * 3600_000L;
        String sql = "INSERT INTO login_bans(user_id, until_ms) VALUES (?, ?) " +
                     "ON CONFLICT (user_id) DO UPDATE SET until_ms = GREATEST(login_bans.until_ms, EXCLUDED.until_ms)";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(userId));
            ps.setLong(2, until);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public boolean isPostingSuspended(String userId) {
        String sql = "SELECT until_ms FROM post_bans WHERE user_id=?";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(userId));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                return rs.getLong(1) > System.currentTimeMillis();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public boolean isLoginSuspended(String userId) {
        String sql = "SELECT until_ms FROM login_bans WHERE user_id=?";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(userId));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                return rs.getLong(1) > System.currentTimeMillis();
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public long postingSuspendedUntil(String userId) {
        String sql = "SELECT until_ms FROM post_bans WHERE user_id=?";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(userId));
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getLong(1) : 0L; }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public long loginSuspendedUntil(String userId) {
        String sql = "SELECT until_ms FROM login_bans WHERE user_id=?";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(userId));
            try (ResultSet rs = ps.executeQuery()) { return rs.next() ? rs.getLong(1) : 0L; }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public void markAsAdmin(String userId) {
        String sql = "INSERT INTO admins(user_id) VALUES (?) ON CONFLICT DO NOTHING";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(userId));
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public boolean isAdmin(String userId) {
        String sql = "SELECT 1 FROM admins WHERE user_id=? LIMIT 1";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(userId));
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        String sql = "SELECT id, first_name, last_name, email, password_hash, city, created_at FROM users WHERE LOWER(email)=LOWER(?)";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(map(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public User save(User u) {
        String sql = "INSERT INTO users(id, first_name, last_name, email, password_hash, city, created_at) " +
                     "VALUES (?,?,?,?,?,?,?) " +
                     "ON CONFLICT (id) DO UPDATE SET first_name=EXCLUDED.first_name, last_name=EXCLUDED.last_name, email=EXCLUDED.email, password_hash=EXCLUDED.password_hash, city=EXCLUDED.city";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(u.getId()));
            ps.setString(2, u.getFirstName());
            ps.setString(3, u.getLastName());
            ps.setString(4, u.getEmail().toLowerCase().trim());
            ps.setString(5, u.getPasswordHash());
            ps.setString(6, u.getCity());
            ps.setTimestamp(7, java.sql.Timestamp.valueOf(u.getCreatedAt()));
            ps.executeUpdate();
            return u;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public Collection<User> findAll() {
        String sql = "SELECT id, first_name, last_name, email, password_hash, city, created_at FROM users";
        List<User> out = new ArrayList<>();
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
            return Collections.unmodifiableList(out);
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public boolean existsById(String id) {
        if (id == null) return false;
        String sql = "SELECT 1 FROM users WHERE id=? LIMIT 1";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(id));
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public Optional<User> findById(String id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT id, first_name, last_name, email, password_hash, city, created_at FROM users WHERE id=?";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(id));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(map(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private User map(ResultSet rs) throws SQLException {
        String id   = rs.getObject("id", java.util.UUID.class).toString();
        String fn   = rs.getString("first_name");
        String ln   = rs.getString("last_name");
        String em   = rs.getString("email");
        String ph   = rs.getString("password_hash");
        String city = rs.getString("city");
        LocalDateTime created = rs.getTimestamp("created_at").toLocalDateTime();

        User u = new User(fn, ln, em, ph, city); // generiše nov UUID → hidriraćemo DB id/createdAt
        util.Hydrator.set(u, "id", id);
        util.Hydrator.set(u, "createdAt", created);
        return u;
    }
}
