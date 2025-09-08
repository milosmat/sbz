package primeri;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collection;
import java.util.regex.Pattern;

import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.runtime.ClassObjectFilter;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import dto.LoginRequest;
import model.User;
import model.ValidationResult;
import repo.UserRepository;
import util.KnowledgeSessionHelper;
import util.PasswordHasher;

public class LoginTest {

    static KieContainer kieContainer;

    @BeforeClass
    public static void beforeClass() {
        kieContainer = KnowledgeSessionHelper.createRuleBase();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void login_praznaPolja_okida3Pravila() {
        KieSession kSession = KnowledgeSessionHelper.getStatefulKnowledgeSession(kieContainer, "test-session");
        try {
            UserRepository repo = new UserRepository();
            kSession.setGlobal("userRepo", repo);
            kSession.setGlobal("EMAIL_RX", Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"));

            LoginRequest req = new LoginRequest("", "");
            ValidationResult vr = new ValidationResult();

            kSession.insert(req);
            kSession.insert(vr);

            kSession.getAgenda().getAgendaGroup("login").setFocus();
            int fired = kSession.fireAllRules();

            // očekujemo: email prazan, lozinka prazna, (email format se ne puca jer je email prazan)
            assertThat(fired, is(2));

            Collection<ValidationResult> vrs =
                (Collection<ValidationResult>) kSession.getObjects(new ClassObjectFilter(ValidationResult.class));
            ValidationResult vrFromSession = vrs.iterator().next();
            String msg = String.join("|", vrFromSession.getErrors());
            assertThat(msg.contains("Unesite mejl"), is(true));
            assertThat(msg.contains("Unesite lozinku"), is(true));
        } finally {
            kSession.dispose();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void login_nepostojeciKorisnik_okida1Pravilo() {
        KieSession kSession = KnowledgeSessionHelper.getStatefulKnowledgeSession(kieContainer, "test-session");
        try {
            UserRepository repo = new UserRepository();
            kSession.setGlobal("userRepo", repo);
            kSession.setGlobal("EMAIL_RX", Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"));

            LoginRequest req = new LoginRequest("nema@primer.com", "tajna123");
            ValidationResult vr = new ValidationResult();

            kSession.insert(req);
            kSession.insert(vr);

            kSession.getAgenda().getAgendaGroup("login").setFocus();
            int fired = kSession.fireAllRules();

            assertThat(fired, is(1)); // "Korisnik ne postoji"

            Collection<ValidationResult> vrs =
                (Collection<ValidationResult>) kSession.getObjects(new ClassObjectFilter(ValidationResult.class));
            ValidationResult vrFromSession = vrs.iterator().next();
            String msg = String.join("|", vrFromSession.getErrors());
            assertThat(msg.contains("Korisnik ne postoji"), is(true));
        } finally {
            kSession.dispose();
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void login_pogresnaLozinka_okida1Pravilo() {
        KieSession kSession = KnowledgeSessionHelper.getStatefulKnowledgeSession(kieContainer, "test-session");
        try {
            UserRepository repo = new UserRepository();
            // u repo upiši korisnika
            User u = new User("Pera", "Peric", "pera@example.com", PasswordHasher.sha256("ispravna"), "Beograd");
            repo.save(u);

            kSession.setGlobal("userRepo", repo);
            kSession.setGlobal("EMAIL_RX", Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"));

            LoginRequest req = new LoginRequest("pera@example.com", "pogresna");
            ValidationResult vr = new ValidationResult();

            kSession.insert(req);
            kSession.insert(vr);

            kSession.getAgenda().getAgendaGroup("login").setFocus();
            int fired = kSession.fireAllRules();

            assertThat(fired, is(1)); // "Pogrešna lozinka"

            Collection<ValidationResult> vrs =
                (Collection<ValidationResult>) kSession.getObjects(new ClassObjectFilter(ValidationResult.class));
            ValidationResult vrFromSession = vrs.iterator().next();
            String msg = String.join("|", vrFromSession.getErrors());
            assertThat(msg.contains("Pogrešna lozinka"), is(true));
        } finally {
            kSession.dispose();
        }
    }

    @Test
    public void login_uspeh_neOkidaNista() {
        KieSession kSession = KnowledgeSessionHelper.getStatefulKnowledgeSession(kieContainer, "test-session");
        try {
            UserRepository repo = new UserRepository();
            User u = new User("Ana", "Anić", "ana@example.com", PasswordHasher.sha256("tajna123"), "Kragujevac");
            repo.save(u);

            kSession.setGlobal("userRepo", repo);
            kSession.setGlobal("EMAIL_RX", Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"));

            LoginRequest req = new LoginRequest("ana@example.com", "tajna123");
            ValidationResult vr = new ValidationResult();

            kSession.insert(req);
            kSession.insert(vr);

            kSession.getAgenda().getAgendaGroup("login").setFocus();
            int fired = kSession.fireAllRules();

            assertThat(fired, is(0));
            assertThat(vr.isOk(), is(true));
        } finally {
            kSession.dispose();
        }
    }
}
