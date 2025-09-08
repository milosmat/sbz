package primeri;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;

import model.User;
import org.junit.Test;
import repo.ModerationEventsRepository;
import repo.UserRepository;
import service.ModerationService;

public class ModerationDetectTest {

    // Pomoćna: resetuje flag listu (događaje ne moramo da čistimo jer koristimo nove userId-ove u svakom testu)
    private void resetFlags(ModerationEventsRepository mr) {
        mr.getFlagsAndClear();
    }

    @Test
    public void reports_6_in_24h_triggers_post_ban_24h() {
        UserRepository ur = new UserRepository();
        ModerationEventsRepository mr = ModerationEventsRepository.getInstance();
        ModerationService svc = new ModerationService(ur, mr);
        resetFlags(mr);

        User a = new User("A","A","a1@e","h","BG"); ur.save(a);
        User r1 = new User("R1","R","r1@e","h","BG"); ur.save(r1);
        User r2 = new User("R2","R","r2@e","h","BG"); ur.save(r2);
        User r3 = new User("R3","R","r3@e","h","BG"); ur.save(r3);
        User r4 = new User("R4","R","r4@e","h","BG"); ur.save(r4);
        User r5 = new User("R5","R","r5@e","h","BG"); ur.save(r5);
        long now = System.currentTimeMillis();

        // 6 prijava u poslednja 24h => >5 -> ban objava 24h
        mr.recordReportAt(a.getId(), r1.getId(), "p1", now - 1_000);
        mr.recordReportAt(a.getId(), r2.getId(), "p2", now - 2_000);
        mr.recordReportAt(a.getId(), r3.getId(), "p3", now - 3_000);
        mr.recordReportAt(a.getId(), r4.getId(), "p4", now - 4_000);
        mr.recordReportAt(a.getId(), r5.getId(), "p5", now - 5_000);
        mr.recordReportAt(a.getId(), r5.getId(), "p6", now - 6_000);

        List<ModerationEventsRepository.Flagged> flags = svc.detectAndSuspend();

        assertThat(ur.isPostingSuspended(a.getId()), is(true));
        assertThat(flags.isEmpty(), is(false));
        assertThat(flags.get(0).reason, containsString("5+ prijava u 24h"));
    }

    @Test
    public void reports_exactly_5_in_24h_does_NOT_trigger() {
        UserRepository ur = new UserRepository();
        ModerationEventsRepository mr = ModerationEventsRepository.getInstance();
        ModerationService svc = new ModerationService(ur, mr);
        resetFlags(mr);

        User a = new User("A","A","a2@e","h","BG"); ur.save(a);
        User r1 = new User("R1","R","r1b@e","h","BG"); ur.save(r1);
        User r2 = new User("R2","R","r2b@e","h","BG"); ur.save(r2);
        User r3 = new User("R3","R","r3b@e","h","BG"); ur.save(r3);
        User r4 = new User("R4","R","r4b@e","h","BG"); ur.save(r4);
        User r5 = new User("R5","R","r5b@e","h","BG"); ur.save(r5);
        long now = System.currentTimeMillis();

        // tačno 5 (granica) -> NE trigeruje jer je pravilo >5
        mr.recordReportAt(a.getId(), r1.getId(), "p1", now - 1_000);
        mr.recordReportAt(a.getId(), r2.getId(), "p2", now - 2_000);
        mr.recordReportAt(a.getId(), r3.getId(), "p3", now - 3_000);
        mr.recordReportAt(a.getId(), r4.getId(), "p4", now - 4_000);
        mr.recordReportAt(a.getId(), r5.getId(), "p5", now - 5_000);

        List<ModerationEventsRepository.Flagged> flags = svc.detectAndSuspend();

        assertThat(ur.isPostingSuspended(a.getId()), is(false));
        assertThat(flags.isEmpty(), is(true));
    }

    @Test
    public void reports_9_in_48h_but_outside_24h_triggers_post_ban_48h_only() {
        UserRepository ur = new UserRepository();
        ModerationEventsRepository mr = ModerationEventsRepository.getInstance();
        ModerationService svc = new ModerationService(ur, mr);
        resetFlags(mr);

        User a = new User("A","A","a3@e","h","BG"); ur.save(a);
        User r = new User("R","R","r@e","h","BG"); ur.save(r);
        long now = System.currentTimeMillis();

        // 9 prijava između 30h i 40h u prošlosti: unutar 48h, ali VAN 24h prozora -> treba 48h ban (ne 24h)
        for (int i = 0; i < 9; i++) {
            long ts = now - (30L * 3_600_000L) - i * 1_000L; // ~30h+
            mr.recordReportAt(a.getId(), r.getId(), "p"+i, ts);
        }

        List<ModerationEventsRepository.Flagged> flags = svc.detectAndSuspend();

        assertThat(ur.isPostingSuspended(a.getId()), is(true));
        assertThat(flags.isEmpty(), is(false));
        assertThat(flags.get(0).reason, containsString("8+ prijava u 48h"));
    }

    @Test
    public void blocks_5_in_24h_triggers_post_ban_24h() {
        UserRepository ur = new UserRepository();
        ModerationEventsRepository mr = ModerationEventsRepository.getInstance();
        ModerationService svc = new ModerationService(ur, mr);
        resetFlags(mr);

        User target = new User("T","User","t@e","h","BG"); ur.save(target);
        User b1 = new User("B1","B","b1@e","h","BG"); ur.save(b1);
        User b2 = new User("B2","B","b2@e","h","BG"); ur.save(b2);
        User b3 = new User("B3","B","b3@e","h","BG"); ur.save(b3);
        User b4 = new User("B4","B","b4@e","h","BG"); ur.save(b4);
        User b5 = new User("B5","B","b5@e","h","BG"); ur.save(b5);
        long now = System.currentTimeMillis();

        mr.recordBlockAt(b1.getId(), target.getId(), now - 1_000);
        mr.recordBlockAt(b2.getId(), target.getId(), now - 2_000);
        mr.recordBlockAt(b3.getId(), target.getId(), now - 3_000);
        mr.recordBlockAt(b4.getId(), target.getId(), now - 4_000);
        mr.recordBlockAt(b5.getId(), target.getId(), now - 5_000);

        List<ModerationEventsRepository.Flagged> flags = svc.detectAndSuspend();

        assertThat(ur.isPostingSuspended(target.getId()), is(true));
        assertThat(flags.isEmpty(), is(false));
        assertThat(flags.get(0).reason, containsString("blokiran"));
    }

    @Test
    public void blocks_3_in_12h_triggers_post_ban_24h() {
        UserRepository ur = new UserRepository();
        ModerationEventsRepository mr = ModerationEventsRepository.getInstance();
        ModerationService svc = new ModerationService(ur, mr);
        resetFlags(mr);

        User target = new User("T","User","t2@e","h","BG"); ur.save(target);
        User b1 = new User("B1","B","b1c@e","h","BG"); ur.save(b1);
        User b2 = new User("B2","B","b2c@e","h","BG"); ur.save(b2);
        User b3 = new User("B3","B","b3c@e","h","BG"); ur.save(b3);
        long now = System.currentTimeMillis();

        // 3 bloka unutar 12h
        mr.recordBlockAt(b1.getId(), target.getId(), now - 1_000);
        mr.recordBlockAt(b2.getId(), target.getId(), now - 2_000);
        mr.recordBlockAt(b3.getId(), target.getId(), now - 3_000);

        List<ModerationEventsRepository.Flagged> flags = svc.detectAndSuspend();

        assertThat(ur.isPostingSuspended(target.getId()), is(true));
        assertThat(flags.isEmpty(), is(false));
        assertThat(flags.get(0).reason, containsString("3+ blokiranja u 12h"));
    }

    @Test
    public void combo_blocks3_in_48h_AND_reports5_in_24h_triggers_login_ban_48h_only() {
        UserRepository ur = new UserRepository();
        ModerationEventsRepository mr = ModerationEventsRepository.getInstance();
        ModerationService svc = new ModerationService(ur, mr);
        resetFlags(mr);

        User a = new User("A","A","a4@e","h","BG"); ur.save(a);
        User b1 = new User("B1","B","b1d@e","h","BG"); ur.save(b1);
        User b2 = new User("B2","B","b2d@e","h","BG"); ur.save(b2);
        User b3 = new User("B3","B","b3d@e","h","BG"); ur.save(b3);
        User r  = new User("R","R","rd@e","h","BG");  ur.save(r);
        long now = System.currentTimeMillis();

        // 3 bloka u 36-44h (unutar 48h; van 24h i van 12h)
        mr.recordBlockAt(b1.getId(), a.getId(), now - 36L * 3_600_000L);
        mr.recordBlockAt(b2.getId(), a.getId(), now - 40L * 3_600_000L);
        mr.recordBlockAt(b3.getId(), a.getId(), now - 44L * 3_600_000L);

        // tačno 5 prijava u 24h (ne aktivira 24h ban jer treba >5, ali zadovoljava deo R4)
        mr.recordReportAt(a.getId(), r.getId(), "p1", now - 1_000);
        mr.recordReportAt(a.getId(), r.getId(), "p2", now - 2_000);
        mr.recordReportAt(a.getId(), r.getId(), "p3", now - 3_000);
        mr.recordReportAt(a.getId(), r.getId(), "p4", now - 4_000);
        mr.recordReportAt(a.getId(), r.getId(), "p5", now - 5_000);

        List<ModerationEventsRepository.Flagged> flags = svc.detectAndSuspend();

        assertThat(ur.isLoginSuspended(a.getId()), is(true));
        assertThat(flags.isEmpty(), is(false));
        assertThat(flags.get(0).reason, containsString("zabrana logovanja 48h"));
        // (opciono) nije neophodno da postoji i post-ban
        assertThat(ur.isPostingSuspended(a.getId()), is(false));
    }

    @Test
    public void reports_11_in_7days_triggers_login_ban_72h() {
        UserRepository ur = new UserRepository();
        ModerationEventsRepository mr = ModerationEventsRepository.getInstance();
        ModerationService svc = new ModerationService(ur, mr);
        resetFlags(mr);

        User a = new User("A","A","a5@e","h","BG"); ur.save(a);
        User r = new User("R","R","r5@e","h","BG");  ur.save(r);

        long now = System.currentTimeMillis();
        // 11 prijava raspoređenih 2–6 dana unazad (van 48h i 24h) -> samo 72h login ban
        for (int i = 0; i < 11; i++) {
            long daysAgo = 2 + (i % 5); // 2..6 dana
            long ts = now - daysAgo * 24L * 3_600_000L - i * 1_000L;
            mr.recordReportAt(a.getId(), r.getId(), "p"+i, ts);
        }

        List<ModerationEventsRepository.Flagged> flags = svc.detectAndSuspend();

        assertThat(ur.isLoginSuspended(a.getId()), is(true));
        assertThat(flags.isEmpty(), is(false));
        assertThat(flags.get(0).reason, containsString("10+ prijava u 7 dana"));
    }

    @Test
    public void edge_case_no_ban_on_exact_thresholds() {
        UserRepository ur = new UserRepository();
        ModerationEventsRepository mr = ModerationEventsRepository.getInstance();
        ModerationService svc = new ModerationService(ur, mr);
        resetFlags(mr);

        User a = new User("A","A","a6@e","h","BG"); ur.save(a);
        User r = new User("R","R","r6@e","h","BG"); ur.save(r);
        User b1 = new User("B1","B","b1e@e","h","BG"); ur.save(b1);
        User b2 = new User("B2","B","b2e@e","h","BG"); ur.save(b2);
        User b3 = new User("B3","B","b3e@e","h","BG"); ur.save(b3);
        User b4 = new User("B4","B","b4e@e","h","BG"); ur.save(b4);

        long now = System.currentTimeMillis();
        long H   = 3_600_000L;

        // tačno 4 prijave ~30h unazad (unutar 48h, van 24h)
        for (int i = 0; i < 4; i++) {
            long ts = now - (30L * H) - i * 1_000L;
            mr.recordReportAt(a.getId(), r.getId(), "p"+i, ts);
        }

        // tačno 4 bloka u 24h, ali svi VAN 12h (13–16h) → ne pogađa R5 (>2 u 12h)
        mr.recordBlockAt(b1.getId(), a.getId(), now - 13L * H);
        mr.recordBlockAt(b2.getId(), a.getId(), now - 14L * H);
        mr.recordBlockAt(b3.getId(), a.getId(), now - 15L * H);
        mr.recordBlockAt(b4.getId(), a.getId(), now - 16L * H);

        // tačno 4 prijave u 24h → deo R4 je >4, pa NE pogađa
        mr.recordReportAt(a.getId(), r.getId(), "p100", now - 10_000);
        mr.recordReportAt(a.getId(), r.getId(), "p101", now - 11_000);
        mr.recordReportAt(a.getId(), r.getId(), "p102", now - 12_000);
        mr.recordReportAt(a.getId(), r.getId(), "p103", now - 13_000);

        // ukupno u 48h = 4 (stare) + 4 (nove) = 8 → tačno na granici R2 (ne puca)
        List<ModerationEventsRepository.Flagged> flags = svc.detectAndSuspend();

        assertThat(ur.isPostingSuspended(a.getId()), is(false));
        assertThat(ur.isLoginSuspended(a.getId()),  is(false));
        assertThat(flags.isEmpty(), is(true));
    }


}
