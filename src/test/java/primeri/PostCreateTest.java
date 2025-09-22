package primeri;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import db.Db;
import dto.CreatePostRequest;
import model.User;
import model.ValidationResult;
import repo.UserRepository;
import util.KnowledgeSessionHelper;

public class PostCreateTest {

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
    public void nije_ulogovan_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kieContainer, "test-session");
        try {
            s.setGlobal("userRepo", new UserRepository());
            ValidationResult vr = new ValidationResult();
            s.insert(new CreatePostRequest("", "tekst", "#sbz"));
            s.insert(vr);
            s.getAgenda().getAgendaGroup("post-create").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1)); // Niste ulogovani.
        } finally { s.dispose(); }
    }

    @Test
    public void korisnik_ne_postoji_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kieContainer, "test-session");
        try {
            s.setGlobal("userRepo", new UserRepository());
            ValidationResult vr = new ValidationResult();
            s.insert(new CreatePostRequest(UUID.randomUUID().toString(), "tekst", "#sbz"));
            s.insert(vr);
            s.getAgenda().getAgendaGroup("post-create").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1)); // Korisnik ne postoji.
        } finally { s.dispose(); }
    }

    @Test
    public void prazan_tekst_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kieContainer, "test-session");
        try {
            UserRepository repo = new UserRepository();
            User u = new User("Pera","Peric","pera@example.com","hash","Beograd");
            repo.save(u);
            s.setGlobal("userRepo", repo);

            ValidationResult vr = new ValidationResult();
            s.insert(new CreatePostRequest(u.getId(), "   ", "#sbz"));
            s.insert(vr);
            s.getAgenda().getAgendaGroup("post-create").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1)); // Tekst objave je obavezan.
        } finally { s.dispose(); }
    }

    @Test
    public void validno_ne_okida_nista() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kieContainer, "test-session");
        try {
            UserRepository repo = new UserRepository();
            User u = new User("Ana","Anić","ana@example.com","hash","Kragujevac");
            repo.save(u);
            s.setGlobal("userRepo", repo);

            ValidationResult vr = new ValidationResult();
            s.insert(new CreatePostRequest(u.getId(), "Zdravo SBZ!", "#sbz #java"));
            s.insert(vr);
            s.getAgenda().getAgendaGroup("post-create").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(0));
            assertThat(vr.isOk(), is(true));
        } finally { s.dispose(); }
    }
}
