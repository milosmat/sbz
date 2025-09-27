package primeri;

import dto.CreatePostRequest;
import dto.RegisterRequest;
import model.Post;
import model.User;
import org.junit.Test;
import repo.FriendRepository;
import repo.PostRepository;
import repo.UserRepository;
import service.FeedService;
import service.PostService;
import service.RegistrationService;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Seeder za testiranje / demonstraciju Feed Recommended pravila.
 *
 * - Kreira korisnike i objave sa hashtagovima
 * - Kreira lajkove tako da pokrije NEW (N1/N3) i BASE (R1-R6) slučajeve
 * - Na kraju poziva FeedService.recommendedFeed za 2 korisnika i proverava da nije prazno
 */
public class SeedFeedRecommendDataTest {

    @Test
    public void seed_feed_recommend_demo_data() {
        UserRepository ur = new UserRepository();
        PostRepository pr = new PostRepository();
        FriendRepository fr = new FriendRepository();

        RegistrationService reg = new RegistrationService(ur);
        PostService post = new PostService(pr, ur);
        FeedService feed = new FeedService(ur, fr, pr);

        // Korisnici
        User targetNew = ur.findByEmail("newuser@sbz.com").orElseGet(() ->
                reg.register(new RegisterRequest("New", "User", "newuser@sbz.com", "lozinka", "Beograd"))
        );
        User targetBase = ur.findByEmail("baseuser@sbz.com").orElseGet(() ->
                reg.register(new RegisterRequest("Base", "User", "baseuser@sbz.com", "lozinka", "Novi Sad"))
        );

        // Autori i crowd za lajkove
        User a1 = ur.findByEmail("author1@sbz.com").orElseGet(() -> reg.register(new RegisterRequest("Autor","Jedan","author1@sbz.com","123456","BG")));
        User a2 = ur.findByEmail("author2@sbz.com").orElseGet(() -> reg.register(new RegisterRequest("Autor","Dva","author2@sbz.com","123456","NS")));
        User a3 = ur.findByEmail("author3@sbz.com").orElseGet(() -> reg.register(new RegisterRequest("Autor","Tri","author3@sbz.com","123456","NS")));

        // grupa korisnika za lajkove (popular post)
        List<User> crowd = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            final int idx = i;
            String em = "crowd"+idx+"@sbz.com";
            User u = ur.findByEmail(em).orElseGet(() -> reg.register(new RegisterRequest("Crowd"+idx, "U", em, "123456", "BG")));
            crowd.add(u);
        }

        // === BASE korisnik: obezbedi router BASE (ima authored post)
        Post baseAuthored = post.createPost(new CreatePostRequest(targetBase.getId(), "Moj prvi #tech post", "#tech"));

        // liked hashtag za BASE (poslednja 3 dana)
        Post t1 = post.createPost(new CreatePostRequest(a1.getId(), "#tech vest 1", "#tech"));
        Post t2 = post.createPost(new CreatePostRequest(a2.getId(), "#tech vest 2", "#tech"));
        Post t3 = post.createPost(new CreatePostRequest(a3.getId(), "#tech vest 3", "#tech"));
        post.likePost(targetBase.getId(), t1.getId());
        post.likePost(targetBase.getId(), t2.getId());
        post.likePost(targetBase.getId(), t3.getId());

        // candidate sa liked hashtagom (R2)
        Post techCandidate = post.createPost(new CreatePostRequest(a1.getId(), "Korisno: #tech predavanje", "#tech"));

        // popularan hešteg (>5 objava u 24h): #popular
        List<Post> popularTagPosts = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            popularTagPosts.add(post.createPost(new CreatePostRequest((i % 2 == 0 ? a2.getId() : a3.getId()), "#popular objava "+i, "#popular")));
        }
        // kandidat sa popularnim heštegom (R5)
        Post popularHashtagCandidate = post.createPost(new CreatePostRequest(a2.getId(), "Trend #popular", "#popular"));

        // popularan post (24h + >10 lajkova):
        Post hot = post.createPost(new CreatePostRequest(a3.getId(), "HOT tema #tech #popular", "#tech #popular"));
        for (User u : crowd) { post.likePost(u.getId(), hot.getId()); }

        // === NEW korisnik: nema prijatelja, nema authored, ali je lajkovao neke postove
        Post lf1 = post.createPost(new CreatePostRequest(a1.getId(), "Film preporuka", "#film"));
        Post lf2 = post.createPost(new CreatePostRequest(a2.getId(), "Još o #film", "#film"));
        Post lf3 = post.createPost(new CreatePostRequest(a3.getId(), "Random #film", "#film"));
        post.likePost(targetNew.getId(), lf1.getId());
        post.likePost(targetNew.getId(), lf2.getId());
        post.likePost(targetNew.getId(), lf3.getId()); // 3+ za UserPreferredTag (#film)

        // sličan korisnik: deli >= 0.5 lajkovanih postova
        User similar = ur.findByEmail("similar@sbz.com").orElseGet(() -> reg.register(new RegisterRequest("Sim","User","similar@sbz.com","123456","BG")));
        // neka zajednička lajkovanja: targetNew {lf1, lf2, lf3}, similar {lf1, lf2, extra}
        post.likePost(similar.getId(), lf1.getId());
        post.likePost(similar.getId(), lf2.getId());
        Post extra = post.createPost(new CreatePostRequest(a1.getId(), "Extra post", "#misc"));
        post.likePost(similar.getId(), extra.getId());

        // kandidat koji je lajkovao sličan korisnik (N1)
        Post recBySimilar = post.createPost(new CreatePostRequest(a2.getId(), "Sličan korisnik voli ovo", "#film"));
        post.likePost(similar.getId(), recBySimilar.getId());

        // kandidat koji odgovara preferenciji korisnika (N3) — #film
        Post recByPref = post.createPost(new CreatePostRequest(a3.getId(), "Preporuka #film", "#film"));

        // sanity: pozovi FeedService.recommendedFeed za oba korisnika
        LocalDateTime now = LocalDateTime.now();
        List<dto.CandidatePost> newRecs  = feed.recommendedFeed(targetNew.getId(), now, 10);
        List<dto.CandidatePost> baseRecs = feed.recommendedFeed(targetBase.getId(), now, 10);

        // (nije obavezno, ali korisno za pregled u izlazu)
        System.out.println("[SEED FEED] NEW user recs:");
        for (dto.CandidatePost c : newRecs) {
            if (c == null || c.getPost() == null) continue;
            System.out.println(" - post="+c.getPost().getId()+" score="+c.getScore()+" reasons="+c.getReasons());
        }
        System.out.println("[SEED FEED] BASE user recs:");
        for (dto.CandidatePost c : baseRecs) {
            if (c == null || c.getPost() == null) continue;
            System.out.println(" - post="+c.getPost().getId()+" score="+c.getScore()+" reasons="+c.getReasons());
        }

        // minimalne asercije — treba da ima bar po 1 preporuku
        assertThat("NEW user treba da dobije preporuke", !newRecs.isEmpty(), is(true));
        assertThat("BASE user treba da dobije preporuke", !baseRecs.isEmpty(), is(true));
    }
}
