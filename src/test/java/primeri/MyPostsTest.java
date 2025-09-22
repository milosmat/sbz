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
import dto.MyPostsRequest;
import model.User;
import model.ValidationResult;
import repo.UserRepository;
import util.KnowledgeSessionHelper;

public class MyPostsTest {

    static KieContainer kieContainer;

    @BeforeClass
    public static void beforeClass() {
        kieContainer = KnowledgeSessionHelper.createRuleBase();
    }
    
    @Before
    public void cleanupbefore() throws Exception {
        try (Connection c = Db.get(); Statement st = c.createStatement()) {
            st.executeUpdate("TRUNCATE users CASCADE");
        }
    }

    @After
    public void cleanup() throws Exception {
        try (Connection c = Db.get(); Statement st = c.createStatement()) {
            st.executeUpdate("TRUNCATE users CASCADE");
        }
    }
    
    @Test
    public void userId_prazan_okida1Pravilo() {
        KieSession kSession = KnowledgeSessionHelper.getStatefulKnowledgeSession(kieContainer, "test-session");
        try {
            UserRepository userRepo = new UserRepository();
            kSession.setGlobal("userRepo", userRepo);

            ValidationResult vr = new ValidationResult();
            kSession.insert(new MyPostsRequest(""));
            kSession.insert(vr);

            kSession.getAgenda().getAgendaGroup("my-posts").setFocus();
            int fired = kSession.fireAllRules();

            assertThat(fired, is(1)); // "Niste ulogovani (userId nedostaje)."
        } finally {
            kSession.dispose();
        }
    }

    @Test
    public void user_ne_postoji_okida1Pravilo() {
        KieSession kSession = KnowledgeSessionHelper.getStatefulKnowledgeSession(kieContainer, "test-session");
        try {
            UserRepository userRepo = new UserRepository(); // prazan
            kSession.setGlobal("userRepo", userRepo);

            ValidationResult vr = new ValidationResult();
            kSession.insert(new MyPostsRequest(UUID.randomUUID().toString()));
            kSession.insert(vr);

            kSession.getAgenda().getAgendaGroup("my-posts").setFocus();
            int fired = kSession.fireAllRules();

            assertThat(fired, is(1)); // "Korisnik ne postoji."
        } finally {
            kSession.dispose();
        }
    }

    @Test
    public void validan_user_ne_okida_pravila() {
        KieSession kSession = KnowledgeSessionHelper.getStatefulKnowledgeSession(kieContainer, "test-session");
        try {
            UserRepository userRepo = new UserRepository();
            User u = new User("Pera","Peric","pera@example.com","hash","Beograd");
            userRepo.save(u);

            kSession.setGlobal("userRepo", userRepo);

            ValidationResult vr = new ValidationResult();
            kSession.insert(new MyPostsRequest(u.getId()));
            kSession.insert(vr);

            kSession.getAgenda().getAgendaGroup("my-posts").setFocus();
            int fired = kSession.fireAllRules();

            assertThat(fired, is(0));
            assertThat(vr.isOk(), is(true));
        } finally {
            kSession.dispose();
        }
    }
}
