package repo;

import db.Db;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class ModerationEventsRepository {

    public static class ReportEvent {
        public final String authorId;
        public final String reporterId;
        public final String postId;
        public final long   ts;
        public ReportEvent(String authorId, String reporterId, String postId, long ts) {
            this.authorId = authorId; this.reporterId = reporterId; this.postId = postId; this.ts = ts;
        }
    }
    public static class BlockEvent {
        public final String blockerId;
        public final String targetId;
        public final long   ts;
        public BlockEvent(String blockerId, String targetId, long ts) {
            this.blockerId = blockerId; this.targetId = targetId; this.ts = ts;
        }
    }
    public static class Flagged {
        public final String userId;
        public final String reason;
        public final long   until;
        public Flagged(String userId, String reason, long until) {
            this.userId = userId; this.reason = reason; this.until = until;
        }
    }

    private static final ModerationEventsRepository INSTANCE = new ModerationEventsRepository();
    public static ModerationEventsRepository getInstance() { return INSTANCE; }

    private ModerationEventsRepository() {}

    // === upis događaja (produkcija) ===
    public void recordReport(String authorId, String reporterId, String postId) {
        recordReportAt(authorId, reporterId, postId, System.currentTimeMillis());
    }
    public void recordBlock(String blockerId, String targetId) {
        recordBlockAt(blockerId, targetId, System.currentTimeMillis());
    }

    // === pomoćne metode za testove (kontrolisan timestamp) ===
    public void recordReportAt(String authorId, String reporterId, String postId, long epochMs) {
        String sql = "INSERT INTO moderation_report_events(author_id, reporter_id, post_id, ts_ms) VALUES (?,?,?,?)";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(authorId));
            ps.setObject(2, java.util.UUID.fromString(reporterId));
            ps.setObject(3, java.util.UUID.fromString(postId));
            ps.setLong(4, epochMs);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }
    public void recordBlockAt(String blockerId, String targetId, long epochMs) {
        String sql = "INSERT INTO moderation_block_events(blocker_id, target_id, ts_ms) VALUES (?,?,?)";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(blockerId));
            ps.setObject(2, java.util.UUID.fromString(targetId));
            ps.setLong(3, epochMs);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    // === flag lista (queue) ===
    public void addFlag(String userId, String reason, long until) {
        String sql = "INSERT INTO moderation_flags(user_id, reason, until_ms) VALUES (?,?,?)";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(userId));
            ps.setString(2, reason);
            ps.setLong(3, until);
            ps.executeUpdate();
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    /** Vrati sve flagove i obriši ih (isti semantički efekat kao in-memory getAndClear) */
    public List<Flagged> getFlagsAndClear() {
        String sel = "SELECT id, user_id, reason, until_ms FROM moderation_flags ORDER BY id";
        String del = "DELETE FROM moderation_flags WHERE id = ANY (?)";
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement(sel);
             ResultSet rs = ps.executeQuery()) {

            List<Flagged> out = new ArrayList<>();
            List<Long> ids = new ArrayList<>();
            while (rs.next()) {
                ids.add(rs.getLong("id"));
                String uid = rs.getObject("user_id", java.util.UUID.class).toString();
                out.add(new Flagged(uid, rs.getString("reason"), rs.getLong("until_ms")));
            }

            if (!ids.isEmpty()) {
                Array arr = c.createArrayOf("bigint", ids.toArray(new Long[0]));
                try (PreparedStatement delPs = c.prepareStatement(del)) {
                    delPs.setArray(1, arr);
                    delPs.executeUpdate();
                }
            }
            return out;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    // === čitanje događaja ===
    public List<ReportEvent> getReports() {
        String sql = "SELECT author_id, reporter_id, post_id, ts_ms FROM moderation_report_events ORDER BY ts_ms";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<ReportEvent> out = new ArrayList<>();
            while (rs.next()) {
                String author   = rs.getObject("author_id", java.util.UUID.class).toString();
                String reporter = rs.getObject("reporter_id", java.util.UUID.class).toString();
                String post     = rs.getObject("post_id", java.util.UUID.class).toString();
                long ts         = rs.getLong("ts_ms");
                out.add(new ReportEvent(author, reporter, post, ts));
            }
            return out;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public List<BlockEvent> getBlocks() {
        String sql = "SELECT blocker_id, target_id, ts_ms FROM moderation_block_events ORDER BY ts_ms";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<BlockEvent> out = new ArrayList<>();
            while (rs.next()) {
                String blocker = rs.getObject("blocker_id", java.util.UUID.class).toString();
                String target  = rs.getObject("target_id", java.util.UUID.class).toString();
                long ts        = rs.getLong("ts_ms");
                out.add(new BlockEvent(blocker, target, ts));
            }
            return out;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    /** Oprez: u produkciji ovo nemoj zvati! Korisno samo za testove/seed. */
    public void clearEvents() {
        try (Connection c = Db.get(); Statement st = c.createStatement()) {
            st.executeUpdate("TRUNCATE moderation_report_events");
            st.executeUpdate("TRUNCATE moderation_block_events");
        } catch (SQLException e) { throw new RuntimeException(e); }
    }
}
