package repo;

import java.time.Instant;
import java.util.*;

/** Singleton koji čuva događaje (prijave/blokiranja) i listu označenih korisnika. */
public class ModerationEventsRepository {

    public static class ReportEvent {
        public final String authorId;   // autor prijavljene objave
        public final String reporterId; // ko je prijavio
        public final String postId;
        public final long   ts;         // epoch millis
        public ReportEvent(String authorId, String reporterId, String postId, long ts) {
            this.authorId = authorId; this.reporterId = reporterId; this.postId = postId; this.ts = ts;
        }
    }
    public static class BlockEvent {
        public final String blockerId;  // ko blokira
        public final String targetId;   // koga blokira (meta)
        public final long   ts;
        public BlockEvent(String blockerId, String targetId, long ts) {
            this.blockerId = blockerId; this.targetId = targetId; this.ts = ts;
        }
    }
    public static class Flagged {
        public final String userId;
        public final String reason;     // tekstualan razlog + sankcija
        public final long   until;      // do kada vazi (epoch millis), 0 ako samo “flag”
        public Flagged(String userId, String reason, long until) {
            this.userId = userId; this.reason = reason; this.until = until;
        }
    }

    private static final ModerationEventsRepository INSTANCE = new ModerationEventsRepository();
    public static ModerationEventsRepository getInstance() { return INSTANCE; }

    private final List<ReportEvent> reports = Collections.synchronizedList(new ArrayList<ReportEvent>());
    private final List<BlockEvent>  blocks  = Collections.synchronizedList(new ArrayList<BlockEvent>());
    private final List<Flagged>     flags   = Collections.synchronizedList(new ArrayList<Flagged>());

    private ModerationEventsRepository() {}

    // === upis događaja (produkcija) ===
    public void recordReport(String authorId, String reporterId, String postId) {
        reports.add(new ReportEvent(authorId, reporterId, postId, Instant.now().toEpochMilli()));
    }
    public void recordBlock(String blockerId, String targetId) {
        blocks.add(new BlockEvent(blockerId, targetId, Instant.now().toEpochMilli()));
    }

    // === pomoćne metode za testove (kontrolisan timestamp) ===
    public void recordReportAt(String authorId, String reporterId, String postId, long epochMs) {
        reports.add(new ReportEvent(authorId, reporterId, postId, epochMs));
    }
    public void recordBlockAt(String blockerId, String targetId, long epochMs) {
        blocks.add(new BlockEvent(blockerId, targetId, epochMs));
    }

    // === queryji (time-window) ===
    public int countReportsAgainstUserInHours(String userId, int hours) {
        long now = System.currentTimeMillis();
        long from = now - hours * 3600_000L;
        int c = 0;
        synchronized (reports) {
            for (ReportEvent e : reports) {
                if (userId.equals(e.authorId) && e.ts >= from) c++;
            }
        }
        return c;
    }

    public int countReportsAgainstUserInDays(String userId, int days) {
        return countReportsAgainstUserInHours(userId, days * 24);
    }

    public int countBlocksAgainstUserInHours(String userId, int hours) {
        long now = System.currentTimeMillis();
        long from = now - hours * 3600_000L;
        int c = 0;
        synchronized (blocks) {
            for (BlockEvent e : blocks) {
                if (userId.equals(e.targetId) && e.ts >= from) c++;
            }
        }
        return c;
    }

    public int countBlocksAgainstUserInDays(String userId, int days) {
        return countBlocksAgainstUserInHours(userId, days * 24);
    }

    // === flag lista ===
    public void addFlag(String userId, String reason, long until) {
        flags.add(new Flagged(userId, reason, until));
    }

    public List<Flagged> getFlagsAndClear() {
        List<Flagged> out = new ArrayList<Flagged>(flags);
        flags.clear();
        return out;
    }
}
