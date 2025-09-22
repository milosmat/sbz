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
import dto.BlockUserRequest;
import model.User;
import model.ValidationResult;
import repo.FriendRepository;
import repo.UserRepository;
import util.KnowledgeSessionHelper;

public class BlockFriendTest {

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
            // kreiraj validnog targeta da ne pukne "target ne postoji"
            UserRepository ur = new UserRepository();
            User t = new User("T","U","t@ex.com","h","BG"); ur.save(t);

            s.setGlobal("userRepo", ur);
            s.setGlobal("friendRepo", new FriendRepository());

            ValidationResult vr = new ValidationResult();
            s.insert(new BlockUserRequest("", t.getId()));
            s.insert(vr);

            s.getAgenda().getAgendaGroup("friend-block").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1));
        } finally { s.dispose(); }
    }

    @Test
    public void targetId_prazan_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            UserRepository ur = new UserRepository();
            User me = new User("A","B","a@b.com","h","C"); ur.save(me);

            s.setGlobal("userRepo", ur);
            s.setGlobal("friendRepo", new FriendRepository());

            ValidationResult vr = new ValidationResult();
            s.insert(new BlockUserRequest(me.getId(), ""));
            s.insert(vr);

            s.getAgenda().getAgendaGroup("friend-block").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1));
        } finally { s.dispose(); }
    }

    @Test
    public void ne_mozes_blokirati_sebe_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            UserRepository ur = new UserRepository();
            User me = new User("Pera","Peric","p@e.com","h","BG"); ur.save(me);

            s.setGlobal("userRepo", ur);
            s.setGlobal("friendRepo", new FriendRepository());

            ValidationResult vr = new ValidationResult();
            s.insert(new BlockUserRequest(me.getId(), me.getId()));
            s.insert(vr);

            s.getAgenda().getAgendaGroup("friend-block").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1));
        } finally { s.dispose(); }
    }

    @Test
    public void target_ne_postoji_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            UserRepository ur = new UserRepository();
            User me = new User("Ana","Anic","ana@e.com","h","NS"); ur.save(me);

            s.setGlobal("userRepo", ur);
            s.setGlobal("friendRepo", new FriendRepository());

            ValidationResult vr = new ValidationResult();
            s.insert(new BlockUserRequest(me.getId(), UUID.randomUUID().toString()));
            s.insert(vr);

            s.getAgenda().getAgendaGroup("friend-block").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1));
        } finally { s.dispose(); }
    }

    @Test
    public void vec_blokiran_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            UserRepository ur = new UserRepository();
            User a = new User("Ana","Anic","ana@ex.com","h","BG"); ur.save(a);
            User b = new User("Mika","Mikic","mika@ex.com","h","BG"); ur.save(b);

            FriendRepository fr = new FriendRepository();
            fr.block(a.getId(), b.getId());

            s.setGlobal("userRepo", ur);
            s.setGlobal("friendRepo", fr);

            ValidationResult vr = new ValidationResult();
            s.insert(new BlockUserRequest(a.getId(), b.getId()));
            s.insert(vr);

            s.getAgenda().getAgendaGroup("friend-block").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1)); // "Već ste blokirali..."
        } finally { s.dispose(); }
    }

    @Test
    public void validno_ne_okida_nista() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            UserRepository ur = new UserRepository();
            User a = new User("Ana","Anic","ana@ex.com","h","BG"); ur.save(a);
            User b = new User("Mika","Mikic","mika@ex.com","h","BG"); ur.save(b);

            s.setGlobal("userRepo", ur);
            s.setGlobal("friendRepo", new FriendRepository());

            ValidationResult vr = new ValidationResult();
            s.insert(new BlockUserRequest(a.getId(), b.getId()));
            s.insert(vr);

            s.getAgenda().getAgendaGroup("friend-block").setFocus();
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
