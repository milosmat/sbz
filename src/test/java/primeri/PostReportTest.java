package primeri;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.Connection;
import java.sql.Statement;
import java.util.Collections;
import java.util.HashSet;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import db.Db;
import dto.ReportPostRequest;
import model.Post;
import model.User;
import model.ValidationResult;
import repo.PostRepository;
import repo.UserRepository;
import util.KnowledgeSessionHelper;

public class PostReportTest {

    static KieContainer kc;

    @BeforeClass
    public static void beforeClass() {
        kc = KnowledgeSessionHelper.createRuleBase();
    }

    @Before
    @After
    public void cleanupbefore() throws Exception {
        try (Connection c = Db.get(); Statement st = c.createStatement()) {
            st.executeUpdate("TRUNCATE users CASCADE");
            st.executeUpdate("TRUNCATE posts CASCADE");
        }
    }
    
    @Test
    public void userId_prazan_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            // validan post da ne puca "post ne postoji"
            UserRepository ur = new UserRepository();
            User author = new User("A","B","a@b.com","h","C"); ur.save(author);
            PostRepository pr = new PostRepository();
            Post p = new Post(author.getId(), "txt", new HashSet<String>()); pr.save(p);

            s.setGlobal("userRepo", ur);
            s.setGlobal("postRepo", pr);

            ValidationResult vr = new ValidationResult();
            s.insert(new ReportPostRequest("", p.getId(), "spam"));
            s.insert(vr);

            s.getAgenda().getAgendaGroup("post-report").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1));
        } finally { s.dispose(); }
    }

    @Test
    public void postId_prazan_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            UserRepository ur = new UserRepository();
            User u = new User("U","S","u@s.com","h","BG"); ur.save(u);
            s.setGlobal("userRepo", ur);
            s.setGlobal("postRepo", new PostRepository());

            ValidationResult vr = new ValidationResult();
            s.insert(new ReportPostRequest(u.getId(), "", "spam"));
            s.insert(vr);

            s.getAgenda().getAgendaGroup("post-report").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1));
        } finally { s.dispose(); }
    }

    @Test
    public void korisnik_ne_postoji_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            UserRepository ur = new UserRepository();
            User author = new User("A","B","a@b.com","h","C"); ur.save(author);
            PostRepository pr = new PostRepository();
            Post p = new Post(author.getId(), "txt", new HashSet<String>()); pr.save(p);

            s.setGlobal("userRepo", ur);
            s.setGlobal("postRepo", pr);

            ValidationResult vr = new ValidationResult();
            s.insert(new ReportPostRequest(UUID.randomUUID().toString(), p.getId(), "spam"));
            s.insert(vr);

            s.getAgenda().getAgendaGroup("post-report").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1));
        } finally { s.dispose(); }
    }

    @Test
    public void post_ne_postoji_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            UserRepository ur = new UserRepository();
            User u = new User("U","S","u@s.com","h","BG"); ur.save(u);

            s.setGlobal("userRepo", ur);
            s.setGlobal("postRepo", new PostRepository());

            ValidationResult vr = new ValidationResult();
            s.insert(new ReportPostRequest(u.getId(), UUID.randomUUID().toString(), "spam"));
            s.insert(vr);

            s.getAgenda().getAgendaGroup("post-report").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1));
        } finally { s.dispose(); }
    }

    @Test
    public void sopstvena_objava_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            UserRepository ur = new UserRepository();
            User author = new User("A","B","a@b.com","h","C"); ur.save(author);
            PostRepository pr = new PostRepository();
            Post p = new Post(author.getId(), "moja", new HashSet<String>()); pr.save(p);

            s.setGlobal("userRepo", ur);
            s.setGlobal("postRepo", pr);

            ValidationResult vr = new ValidationResult();
            s.insert(new ReportPostRequest(author.getId(), p.getId(), "self"));
            s.insert(vr);

            s.getAgenda().getAgendaGroup("post-report").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1)); // "Ne možete prijaviti sopstvenu objavu."
        } finally { s.dispose(); }
    }

    @Test
    public void vec_prijavljena_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            UserRepository ur = new UserRepository();
            User author = new User("A","B","a@b.com","h","C"); ur.save(author);
            User reporter = new User("R","R","r@r.com","h","C"); ur.save(reporter);

            PostRepository pr = new PostRepository();
            Post p = new Post(author.getId(), "txt", new HashSet<String>()); pr.save(p);
            pr.report(p.getId(), reporter.getId()); // već prijavljeno

            s.setGlobal("userRepo", ur);
            s.setGlobal("postRepo", pr);

            ValidationResult vr = new ValidationResult();
            s.insert(new ReportPostRequest(reporter.getId(), p.getId(), "spam"));
            s.insert(vr);

            s.getAgenda().getAgendaGroup("post-report").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1)); // "Već ste prijavili..."
        } finally { s.dispose(); }
    }

    @Test
    public void validno_ne_okida_nista() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            UserRepository ur = new UserRepository();
            User author = new User("A","B","a@b.com","h","C"); ur.save(author);
            User reporter = new User("R","R","r@r.com","h","C"); ur.save(reporter);

            PostRepository pr = new PostRepository();
            Post p = new Post(author.getId(), "ok", Collections.<String>emptySet()); pr.save(p);

            s.setGlobal("userRepo", ur);
            s.setGlobal("postRepo", pr);

            ValidationResult vr = new ValidationResult();
            s.insert(new ReportPostRequest(reporter.getId(), p.getId(), "spam"));
            s.insert(vr);

            s.getAgenda().getAgendaGroup("post-report").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(0));
        } finally { s.dispose(); }
    }
}
