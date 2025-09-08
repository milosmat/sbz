package primeri;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import dto.LikePostRequest;
import model.Post;
import model.User;
import model.ValidationResult;
import repo.PostRepository;
import repo.UserRepository;
import util.KnowledgeSessionHelper;

import java.util.Collections;
import java.util.HashSet;

public class PostLikeTest {

    static KieContainer kieContainer;

    @BeforeClass
    public static void beforeClass() {
        kieContainer = KnowledgeSessionHelper.createRuleBase();
    }

    @Test
    public void userId_prazan_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kieContainer, "test-session");
        try {
            // Napravi POST da ne bi "objava ne postoji" takođe pucala
            UserRepository ur = new UserRepository();
            User u = new User("A","B","a@b.com","hash","City"); ur.save(u);
            PostRepository pr = new PostRepository();
            Post p = new Post(u.getId(), "txt", new HashSet<String>()); pr.save(p);

            s.setGlobal("userRepo", ur);
            s.setGlobal("postRepo", pr);

            ValidationResult vr = new ValidationResult();
            // userId prazan, postId VALIDAN -> očekujemo SAMO 1 pravilo
            s.insert(new LikePostRequest("", p.getId()));
            s.insert(vr);

            s.getAgenda().getAgendaGroup("post-like").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1));
        } finally { s.dispose(); }
    }

    @Test
    public void postId_prazan_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kieContainer, "test-session");
        try {
            // Napravi USER da ne bi "korisnik ne postoji" dodatno pucao
            UserRepository ur = new UserRepository();
            User u = new User("A","B","a@b.com","hash","City"); ur.save(u);

            s.setGlobal("userRepo", ur);
            s.setGlobal("postRepo", new PostRepository());

            ValidationResult vr = new ValidationResult();
            // userId VALIDAN, postId prazan -> očekujemo SAMO 1 pravilo
            s.insert(new LikePostRequest(u.getId(), ""));
            s.insert(vr);

            s.getAgenda().getAgendaGroup("post-like").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1));
        } finally { s.dispose(); }
    }

    @Test
    public void korisnik_ne_postoji_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kieContainer, "test-session");
        try {
            // Napravi POST da ne bi "objava ne postoji" pucao
            UserRepository ur = new UserRepository();
            User owner = new User("X","Y","x@y.com","h","C"); ur.save(owner);
            PostRepository pr = new PostRepository();
            Post p = new Post(owner.getId(), "txt", new HashSet<String>()); pr.save(p);

            s.setGlobal("userRepo", ur);
            s.setGlobal("postRepo", pr);

            ValidationResult vr = new ValidationResult();
            // nepostojeći, ali ne-prazan userId + postojeći postId
            s.insert(new LikePostRequest("nepostojeci-user", p.getId()));
            s.insert(vr);

            s.getAgenda().getAgendaGroup("post-like").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1));
        } finally { s.dispose(); }
    }

    @Test
    public void post_ne_postoji_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kieContainer, "test-session");
        try {
            UserRepository ur = new UserRepository();
            User u = new User("A","B","a@b.com","hash","City"); ur.save(u);

            s.setGlobal("userRepo", ur);
            s.setGlobal("postRepo", new PostRepository());

            ValidationResult vr = new ValidationResult();
            // postojeći user + nepostojeći post -> 1 pravilo
            s.insert(new LikePostRequest(u.getId(), "no-post"));
            s.insert(vr);

            s.getAgenda().getAgendaGroup("post-like").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1));
        } finally { s.dispose(); }
    }

    @Test
    public void vec_lajkovano_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kieContainer, "test-session");
        try {
            UserRepository ur = new UserRepository();
            User u = new User("Ana","Anic","ana@ex.com","hash","BG"); ur.save(u);

            PostRepository pr = new PostRepository();
            Post p = new Post(u.getId(), "Txt", new HashSet<String>()); pr.save(p);
            // simuliraj već lajkovano
            pr.like(p.getId(), u.getId());

            s.setGlobal("userRepo", ur);
            s.setGlobal("postRepo", pr);

            ValidationResult vr = new ValidationResult();
            s.insert(new LikePostRequest(u.getId(), p.getId()));
            s.insert(vr);

            s.getAgenda().getAgendaGroup("post-like").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1)); // "Već ste lajkovali ovu objavu."
        } finally { s.dispose(); }
    }

    @Test
    public void validno_ne_okida_pravila() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kieContainer, "test-session");
        try {
            UserRepository ur = new UserRepository();
            User u = new User("Ana","Anic","ana@ex.com","hash","BG"); ur.save(u);

            PostRepository pr = new PostRepository();
            Post p = new Post(u.getId(), "Pozdrav", Collections.<String>emptySet()); pr.save(p);

            s.setGlobal("userRepo", ur);
            s.setGlobal("postRepo", pr);

            ValidationResult vr = new ValidationResult();
            s.insert(new LikePostRequest(u.getId(), p.getId()));
            s.insert(vr);

            s.getAgenda().getAgendaGroup("post-like").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(0));
        } finally { s.dispose(); }
    }
}
