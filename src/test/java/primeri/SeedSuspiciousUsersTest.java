package primeri;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.List;
import java.util.UUID;

import model.User;
import org.junit.Test;

import repo.ModerationEventsRepository;
import repo.ModerationEventsRepository.Flagged;
import repo.UserRepository;
import service.ModerationService;
import dto.RegisterRequest;
import service.RegistrationService;


public class SeedSuspiciousUsersTest {

    private UserRepository ur;
    private ModerationEventsRepository mr;
    private ModerationService mod;

    @Test
    public void seed_suspicious_users() {
        ur  = new UserRepository();
        mr  = ModerationEventsRepository.getInstance();
        mod = new ModerationService(ur, mr);

        // === KREIRANJE KORISNIKA PREKO REGISTER SERVISA (kao ensureAdminSeed) ===
        RegistrationService reg = new RegistrationService(ur); // prilagodi konstruktor ako traži još dependencija

        User victim1  = reg.register(new RegisterRequest("V1", "User", "v1@gmail.com", "secret1", "BG"));
        User victim2  = reg.register(new RegisterRequest("V2", "User", "v2@gmail.com", "secret1", "NS"));
        User victim3  = reg.register(new RegisterRequest("V3", "User", "v3@gmail.com", "secret1", "NS")); // combo
        User victim4  = reg.register(new RegisterRequest("V4", "User", "v4@gmail.com", "secret1", "BG")); // 3 bloka/12h
        User reporter = reg.register(new RegisterRequest("R",  "User", "reporter@gmail.com", "secret1", "BG"));
        User b1       = reg.register(new RegisterRequest("B1", "User", "b1@gmail.com", "secret1", "BG"));
        User b2       = reg.register(new RegisterRequest("B2", "User", "b2@gmail.com", "secret1", "BG"));
        User b3       = reg.register(new RegisterRequest("B3", "User", "b3@gmail.com", "secret1", "BG"));
        User b4       = reg.register(new RegisterRequest("B4", "User", "b4@gmail.com", "secret1", "BG"));

        User r1u = reg.register(new RegisterRequest("R1", "Case", "r1@sbz.com", "secret1", "BG")); // R1: 5+ reports/24h
        User r2u = reg.register(new RegisterRequest("R2", "Case", "r2@sbz.com", "secret1", "BG")); // R2: 8+ reports/48h (izvan 24h)
        User r3u = reg.register(new RegisterRequest("R3", "Case", "r3@sbz.com", "secret1", "BG")); // R3: 4+ blocks/24h
        User r4u = reg.register(new RegisterRequest("R4", "Case", "r4@sbz.com", "secret1", "BG")); // R4: (2+ blocks/48h) AND (4+ reports/24h)
        User r5u = reg.register(new RegisterRequest("R5", "Case", "r5@sbz.com", "secret1", "BG")); // R5: 3+ blocks/12h
        User r6u = reg.register(new RegisterRequest("R6", "Case", "r6@sbz.com", "secret1", "BG")); // R6: 10+ reports/7d

        // EDGE/NEGATIVE (ne bi smeli da trigeruju)
        User eR1  = reg.register(new RegisterRequest("E1", "Edge", "e1@sbz.com", "secret1", "BG")); // tačno 5 prijava/24h -> NO
        User eR2  = reg.register(new RegisterRequest("E2", "Edge", "e2@sbz.com", "secret1", "BG")); // tačno 8 prijava/48h (0 u 24h) -> NO
        User eR3  = reg.register(new RegisterRequest("E3", "Edge", "e3@sbz.com", "secret1", "BG")); // tačno 4 bloka/24h -> NO
        User eR4a = reg.register(new RegisterRequest("E4a","Edge", "e4a@sbz.com","secret1","BG")); // 2 blocks/48h + 5 reports/24h -> NO (blocks nisu >2)
        User eR4b = reg.register(new RegisterRequest("E4b","Edge", "e4b@sbz.com","secret1","BG")); // 3 blocks/48h + 4 reports/24h -> NO (reports nisu >4)
        User eR5  = reg.register(new RegisterRequest("E5", "Edge", "e5@sbz.com", "secret1", "BG")); // 2 bloka/12h -> NO
        User clean = reg.register(new RegisterRequest("CL", "Clean","clean@sbz.com","secret1","BG")); // bez događaja

        // dodatni "blocker" nalozi da možemo 5+ blokova
        User b5 = reg.register(new RegisterRequest("B5","User","b5@gmail.com","secret1","BG"));
        User b6 = reg.register(new RegisterRequest("B6","User","b6@gmail.com","secret1","BG"));
        User b7 = reg.register(new RegisterRequest("B7","User","b7@gmail.com","secret1","BG"));
        User b8 = reg.register(new RegisterRequest("B8","User","b8@gmail.com","secret1","BG"));
        
        long now = System.currentTimeMillis();
        long H   = 3_600_000L;

     // ===== R1: >5 prijava u 24h (npr. 6 u poslednjih 60 minuta) -> post ban 24h
        for (int i = 0; i < 6; i++) {
            mr.recordReportAt(r1u.getId(), reporter.getId(), UUID.randomUUID().toString(), now - (i+1)*10_000L);
        }

        // ===== R2: >8 prijava u 48h (ali izvan 24h) -> post ban 48h
        // 9 prijava u intervalu 30–40h unazad (da ne pogodi R1)
        for (int i = 0; i < 9; i++) {
            long ts = now - (30L * H) - i * 2_000L;
            mr.recordReportAt(r2u.getId(), reporter.getId(), UUID.randomUUID().toString(), ts);
        }

        // ===== R3: >4 blokiranja u 24h -> post ban 24h (5 blokova u zadnjih ~10h)
        String[] blockersR3 = { b1.getId(), b2.getId(), b3.getId(), b4.getId(), b5.getId() };
        for (int i = 0; i < blockersR3.length; i++) {
            mr.recordBlockAt(blockersR3[i], r3u.getId(), now - (i+1) * (2L * H)); // 2h razmaka, sve <24h
        }

        // ===== R4: (2+ blocks/48h) AND (4+ reports/24h) -> login ban 48h
        // 3 bloka u 30–40h unazad + 5 prijava u poslednjih par sati
        mr.recordBlockAt(b1.getId(), r4u.getId(), now - 36L * H);
        mr.recordBlockAt(b2.getId(), r4u.getId(), now - 32L * H);
        mr.recordBlockAt(b3.getId(), r4u.getId(), now - 31L * H);
        for (int i = 0; i < 5; i++) {
            mr.recordReportAt(r4u.getId(), reporter.getId(), UUID.randomUUID().toString(), now - (i+1) * 15_000L);
        }

        // ===== R5: 3+ blokiranja u 12h -> post ban 24h
        mr.recordBlockAt(b1.getId(), r5u.getId(), now - 6L * H);
        mr.recordBlockAt(b2.getId(), r5u.getId(), now - 4L * H);
        mr.recordBlockAt(b3.getId(), r5u.getId(), now - 1L * H);

        // ===== R6: 10+ prijava u 7 dana -> login ban 72h
        // rasporedi 11 prijava u poslednjih ~6 dana bez koncentracije >5 u 24h
        for (int i = 0; i < 11; i++) {
            long ts = now - (150L * 60L * 60L * 1000L) + i * (15L * 60L * 60L * 1000L); // od ~-150h, korak 15h
            // osiguraj da je unutar 7d (168h)
            if (now - ts <= 168L * H) {
                mr.recordReportAt(r6u.getId(), reporter.getId(), UUID.randomUUID().toString(), ts);
            }
        }

        // ===== EDGE/NEGATIVE slučajevi =====

        // eR1: TAČNO 5 prijava u 24h -> NE treba da trigeruje (jer pravilo je >5)
        for (int i = 0; i < 5; i++) {
            mr.recordReportAt(eR1.getId(), reporter.getId(), UUID.randomUUID().toString(), now - (i+1) * 20_000L);
        }

        // eR2: TAČNO 8 prijava u 48h, sve između 30–40h -> NE trigeruje (pravilo je >8)
        for (int i = 0; i < 8; i++) {
            long ts = now - (35L * H) - i * 2_000L;
            mr.recordReportAt(eR2.getId(), reporter.getId(), UUID.randomUUID().toString(), ts);
        }

        // eR3: TAČNO 4 blokiranja u 24h -> NE trigeruje (pravilo je >4)
        mr.recordBlockAt(b1.getId(), eR3.getId(), now - 20L * 60L * 60L * 1000L); // -20h
        mr.recordBlockAt(b2.getId(), eR3.getId(), now - 18L * 60L * 60L * 1000L);
        mr.recordBlockAt(b3.getId(), eR3.getId(), now - 12L * 60L * 60L * 1000L);
        mr.recordBlockAt(b4.getId(), eR3.getId(), now -  6L * 60L * 60L * 1000L);

        // eR4a: 2 blocks/48h (nije >2) + 5 reports/24h -> NE trigeruje
        mr.recordBlockAt(b1.getId(), eR4a.getId(), now - 40L * H);
        mr.recordBlockAt(b2.getId(), eR4a.getId(), now - 30L * H);
        for (int i = 0; i < 5; i++) {
            mr.recordReportAt(eR4a.getId(), reporter.getId(), UUID.randomUUID().toString(), now - (i+1) * 25_000L);
        }

        // eR4b: 3 blocks/48h (OK) + 4 reports/24h (nije >4) -> NE trigeruje
        mr.recordBlockAt(b1.getId(), eR4b.getId(), now - 45L * H);
        mr.recordBlockAt(b2.getId(), eR4b.getId(), now - 40L * H);
        mr.recordBlockAt(b3.getId(), eR4b.getId(), now - 28L * H);
        for (int i = 0; i < 4; i++) {
            mr.recordReportAt(eR4b.getId(), reporter.getId(), UUID.randomUUID().toString(), now - (i+1) * 30_000L);
        }

        // eR5: TAČNO 2 bloka u 12h -> NE trigeruje (pravilo je >2)
        mr.recordBlockAt(b1.getId(), eR5.getId(), now - 10L * 60L * 60L * 1000L); // -10h
        mr.recordBlockAt(b2.getId(), eR5.getId(), now -  3L * 60L * 60L * 1000L); // -3h
        
        // Pokreni detekciju i primeni suspenzije
        List<Flagged> flags = mod.detectAndSuspend();
        for (Flagged f : flags) {
            String r = f.reason == null ? "" : f.reason.toLowerCase();
            if (r.contains("logovanja")) {
                long hours = Math.max(1, (f.until - System.currentTimeMillis()) / 3_600_000L);
                ur.suspendLoginHours(f.userId, (int) hours);
            } else {
                // BUGFIX: ranije si prosleđivao epoch kao "hours"
                long hours = Math.max(1, (f.until - System.currentTimeMillis()) / 3_600_000L);
                ur.suspendPostingHours(f.userId, (int) hours);
            }
        }

        assertThat("trebalo bi da ima bar 1 flag", !flags.isEmpty(), is(true));
    }
}
