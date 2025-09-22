package primeri;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Collection;
import java.util.regex.Pattern;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.runtime.ClassObjectFilter;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import db.Db;
import dto.RegisterRequest;
import model.User;
import model.ValidationResult;
import repo.UserRepository;
import util.KnowledgeSessionHelper;
import util.PasswordHasher;

public class RegistrationServiceTest {

    static KieContainer kieContainer;

    @BeforeClass
    public static void beforeClass() {
        kieContainer = KnowledgeSessionHelper.createRuleBase();
    }
    
    @Before
    @After
    public void cleanupbefore() throws Exception {
        try (Connection c = Db.get(); Statement st = c.createStatement()) {
            st.executeUpdate("TRUNCATE users CASCADE");
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void invalidnaPolja_okida5Pravila() {
        KieSession kSession = KnowledgeSessionHelper.getStatefulKnowledgeSession(kieContainer, "test-session");

        try {
            // globali
            UserRepository repo = new UserRepository();
            kSession.setGlobal("userRepo", repo);
            kSession.setGlobal("EMAIL_RX", Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"));

            // fakti: sve loše (prazno ime, prezime, mesto; email prazan; lozinka kratka)
            RegisterRequest req = new RegisterRequest("", "", "", "123", "");
            ValidationResult vr = new ValidationResult();

            kSession.insert(req);
            kSession.insert(vr);

            // fokusiraj “registration”
            kSession.getAgenda().getAgendaGroup("registration").setFocus();
            int fired = kSession.fireAllRules();

            // 5 očekivanih pravila:
            // Ime obavezno, Prezime obavezno, Mesto obavezno, Email prazan, Lozinka min duzina
            assertThat(fired, is(5));

            // ValidationResult bi trebalo da je ostao u sesiji – pokupimo ga filterom (kao u primeru)
            Collection<ValidationResult> vrs =
                (Collection<ValidationResult>) kSession.getObjects(new ClassObjectFilter(ValidationResult.class));
            assertThat(vrs.size(), is(1));

            ValidationResult vrFromSession = vrs.iterator().next();
            String joined = String.join(" | ", vrFromSession.getErrors());
            assertThat(joined.contains("Ime je obavezno"), is(true));
            assertThat(joined.contains("Prezime je obavezno"), is(true));
            assertThat(joined.contains("Mesto stanovanja je obavezno"), is(true));
            assertThat(joined.contains("Neispravan mejl"), is(true));
            assertThat(joined.contains("Lozinka mora imati najmanje 6 karaktera"), is(true));
        } finally {
            kSession.dispose();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void duplikatEmaila_okidaSamo1Pravilo() {
        KieSession kSession = KnowledgeSessionHelper.getStatefulKnowledgeSession(kieContainer, "test-session");

        try {
            // repo sa već postojećim korisnikom
            UserRepository repo = new UserRepository();
            String email = "pera@example.com";
            User vecPostoji = new User("Pera", "Peric", email, PasswordHasher.sha256("tajna123"), "Beograd");
            repo.save(vecPostoji);

            kSession.setGlobal("userRepo", repo);
            kSession.setGlobal("EMAIL_RX", Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"));

            // validan zahtev, ali sa istim mejlom -> treba da se okine SAMO "Email zauzet"
            RegisterRequest req = new RegisterRequest("Mika", "Mikic", email, "druga123", "Novi Sad");
            ValidationResult vr = new ValidationResult();

            kSession.insert(req);
            kSession.insert(vr);

            kSession.getAgenda().getAgendaGroup("registration").setFocus();
            int fired = kSession.fireAllRules();

            assertThat(fired, is(1)); // samo jedno pravilo

            // izvuci ValidationResult iz sesije
            Collection<ValidationResult> vrs =
                (Collection<ValidationResult>) kSession.getObjects(new ClassObjectFilter(ValidationResult.class));
            assertThat(vrs.size(), is(1));
            ValidationResult vrFromSession = vrs.iterator().next();

            String joined = String.join(" | ", vrFromSession.getErrors());
            assertThat(joined.contains("Mejl je već zauzet"), is(true));

            // nema drugih grešaka (ostala polja su bila ok)
            assertThat(joined.contains("Ime je obavezno"), is(false));
            assertThat(joined.contains("Prezime je obavezno"), is(false));
            assertThat(joined.contains("Mesto stanovanja je obavezno"), is(false));
            assertThat(joined.contains("Neispravan mejl"), is(false));
            assertThat(joined.contains("Lozinka mora imati najmanje 6 karaktera"), is(false));
        } finally {
            kSession.dispose();
        }
    }

    @Test
    public void validnaRegistracija_neOkidaNista() {
        KieSession kSession = KnowledgeSessionHelper.getStatefulKnowledgeSession(kieContainer, "test-session");

        try {
            UserRepository repo = new UserRepository();
            kSession.setGlobal("userRepo", repo);
            kSession.setGlobal("EMAIL_RX", Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"));

            RegisterRequest req = new RegisterRequest("Ana", "Anić", "ana@example.com", "tajna123", "Kragujevac");
            ValidationResult vr = new ValidationResult();

            kSession.insert(req);
            kSession.insert(vr);

            kSession.getAgenda().getAgendaGroup("registration").setFocus();
            int fired = kSession.fireAllRules();

            // bez grešaka -> ni jedno pravilo ne “puca”
            assertThat(fired, is(0));

            // i ValidationResult mora biti “ok”
            assertThat(vr.isOk(), is(true));
            assertThat(vr.getErrors().isEmpty(), is(true));
        } finally {
            kSession.dispose();
        }
    }
}
