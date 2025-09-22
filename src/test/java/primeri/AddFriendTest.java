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
import dto.AddFriendRequest;
import model.User;
import model.ValidationResult;
import repo.FriendRepository;
import repo.UserRepository;
import util.KnowledgeSessionHelper;

public class AddFriendTest {

    static KieContainer kc;

    @BeforeClass
    public static void beforeClass() {
        kc = KnowledgeSessionHelper.createRuleBase();
    }
    
    @Before
    public void cleanupbefore() throws Exception {
        try (Connection c = Db.get(); Statement st = c.createStatement()) {
            st.executeUpdate("TRUNCATE users CASCADE");
            st.executeUpdate("TRUNCATE friendships CASCADE");
        }
    }
    
    @Test
    public void userId_prazan_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            // Napravi VALIDNOG target korisnika da "ciljani ne postoji" NE bi pucao
            UserRepository ur = new UserRepository();
            User target = new User("Target","User","t@ex.com","h","BG");
            ur.save(target);

            FriendRepository fr = new FriendRepository();

            s.setGlobal("userRepo", ur);
            s.setGlobal("friendRepo", fr);

            ValidationResult vr = new ValidationResult();
            // userId prazan, ali targetId VALIDAN -> očekujemo da pukne SAMO 1 pravilo
            s.insert(new AddFriendRequest("", target.getId()));
            s.insert(vr);

            s.getAgenda().getAgendaGroup("friend-add").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1));
        } finally { s.dispose(); }
    }


    @Test
    public void targetId_prazan_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            UserRepository ur = new UserRepository();
            User u = new User("A","B","a@b.com","h","C"); ur.save(u);

            s.setGlobal("userRepo", ur);
            s.setGlobal("friendRepo", new FriendRepository());

            ValidationResult vr = new ValidationResult();
            s.insert(new AddFriendRequest(u.getId(), ""));
            s.insert(vr);

            s.getAgenda().getAgendaGroup("friend-add").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1));
        } finally { s.dispose(); }
    }

    @Test
    public void ne_mozes_sebe_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            UserRepository ur = new UserRepository();
            User u = new User("Pera","Peric","p@e.com","h","BG"); ur.save(u);

            s.setGlobal("userRepo", ur);
            s.setGlobal("friendRepo", new FriendRepository());

            ValidationResult vr = new ValidationResult();
            s.insert(new AddFriendRequest(u.getId(), u.getId()));
            s.insert(vr);

            s.getAgenda().getAgendaGroup("friend-add").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1));
        } finally { s.dispose(); }
    }

    @Test
    public void target_ne_postoji_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            UserRepository ur = new UserRepository();
            User u = new User("Ana","Anic","a@e.com","h","NS"); ur.save(u);

            s.setGlobal("userRepo", ur);
            s.setGlobal("friendRepo", new FriendRepository());

            ValidationResult vr = new ValidationResult();
            s.insert(new AddFriendRequest(u.getId(), UUID.randomUUID().toString()));
            s.insert(vr);

            s.getAgenda().getAgendaGroup("friend-add").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1));
        } finally { s.dispose(); }
    }

    @Test
    public void vec_prijatelji_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            UserRepository ur = new UserRepository();
            User u1 = new User("Ana","Anic","ana@ex.com","h","BG"); ur.save(u1);
            User u2 = new User("Mika","Mikic","mika@ex.com","h","BG"); ur.save(u2);

            FriendRepository fr = new FriendRepository();
            fr.addFriends(u1.getId(), u2.getId());

            s.setGlobal("userRepo", ur);
            s.setGlobal("friendRepo", fr);

            ValidationResult vr = new ValidationResult();
            s.insert(new AddFriendRequest(u1.getId(), u2.getId()));
            s.insert(vr);

            s.getAgenda().getAgendaGroup("friend-add").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1)); // "Već ste prijatelji."
        } finally { s.dispose(); }
    }

    @Test
    public void validno_ne_okida_nista() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            UserRepository ur = new UserRepository();
            User u1 = new User("Ana","Anic","ana@ex.com","h","BG"); ur.save(u1);
            User u2 = new User("Mika","Mikic","mika@ex.com","h","BG"); ur.save(u2);

            s.setGlobal("userRepo", ur);
            s.setGlobal("friendRepo", new FriendRepository());

            ValidationResult vr = new ValidationResult();
            s.insert(new AddFriendRequest(u1.getId(), u2.getId()));
            s.insert(vr);

            s.getAgenda().getAgendaGroup("friend-add").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(0));
        } finally { s.dispose(); }
    }
    
    @After
    public void cleanup() throws Exception {
        try (Connection c = Db.get(); Statement st = c.createStatement()) {
            st.executeUpdate("TRUNCATE users CASCADE");
            st.executeUpdate("TRUNCATE friendships CASCADE");
        }
    }
}
