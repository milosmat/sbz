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

    // === flag lista ===
    public void addFlag(String userId, String reason, long until) {
        flags.add(new Flagged(userId, reason, until));
    }

    public List<Flagged> getFlagsAndClear() {
        List<Flagged> out = new ArrayList<Flagged>(flags);
        flags.clear();
        return out;
    }
    
    public List<ReportEvent> getReports() {
        return new ArrayList<>(reports);
    }

    public List<BlockEvent> getBlocks() {
        return new ArrayList<>(blocks);
    }
    
    public void clearEvents() {
        reports.clear();
        blocks.clear();
    }
    
}
