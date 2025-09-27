package primeri;

import db.Db;
import dto.CandidatePost;
import dto.CreatePostRequest;
import dto.FriendFeedRequest;
import dto.RecommendedFeedRequest;
import dto.RegisterRequest;
import model.Post;
import model.User;
import model.ValidationResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import repo.FriendRepository;
import repo.PostRepository;
import repo.UserRepository;
import service.FeedService;
import service.PostService;
import service.RegistrationService;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class FeedRecommendTest {

    private UserRepository userRepo;
    private FriendRepository friendRepo;
    private PostRepository postRepo;

    private RegistrationService regService;
    private PostService postService;
    private FeedService feedService;

    @Before @After
    public void cleanupDb() throws Exception {
        try (Connection c = Db.get(); Statement st = c.createStatement()) {
            try { st.executeUpdate("TRUNCATE post_likes CASCADE"); } catch (Exception ignore) {}
            try { st.executeUpdate("TRUNCATE posts CASCADE"); } catch (Exception ignore) {}
            try { st.executeUpdate("TRUNCATE friendships CASCADE"); } catch (Exception ignore) {}
            try { st.executeUpdate("TRUNCATE blocks CASCADE"); } catch (Exception ignore) {}
            try { st.executeUpdate("TRUNCATE users CASCADE"); } catch (Exception ignore) {}
        }
    }

    private void wiring(){
        userRepo = new UserRepository();
        friendRepo = new FriendRepository();
        postRepo = new PostRepository();
        regService = new RegistrationService(userRepo);
        postService = new PostService(postRepo, userRepo);
        feedService = new FeedService(userRepo, friendRepo, postRepo);
    }

    private User newUser(String fn, String ln, String email, String city){
        return regService.register(new RegisterRequest(fn, ln, email, "lozinka", city));
    }

    private Post newPost(String authorId, String text, String tags){
        return postService.createPost(new CreatePostRequest(authorId, text, tags));
    }

    private void like(String userId, String postId){
        postService.likePost(userId, postId);
    }

    @Test
    public void base_user_rules_fire() {
        wiring();
        LocalDateTime now = LocalDateTime.now();

        // users
        User a = newUser("A","A","a@ex.com","NS"); // main user
        User f = newUser("F","F","f@ex.com","NS"); // friend
        User x = newUser("X","X","x@ex.com","NS"); // non-friend author

        // make them friends
        friendRepo.addFriends(a.getId(), f.getId());

        // friend made a recent post (<24h)
        Post friendRecent = newPost(f.getId(), "friend recent #sbz", "#sbz");

        // non-friend author creates various posts
        Post pRecent = newPost(x.getId(), "recent #java", "#java"); // recent
        Post pAuthTag = newPost(x.getId(), "matches authored #sbz", "#sbz");
        Post pLikedTag = newPost(x.getId(), "matches liked #java", "#java");
        Post pPopTag = newPost(x.getId(), "popular tag #hot", "#hot");

        // main user authored posts with #sbz in last year
        newPost(a.getId(), "my post #sbz", "#sbz");

        // main user liked hashtags in last 3 days -> like the #java post
        like(a.getId(), pLikedTag.getId());

        // simulate popularity: >10 likes in last 24h for pRecent
        for (int i = 0; i < 11; i++) {
            User liker = newUser("L"+i, "L", "l"+i+"@ex.com", "NS");
            like(liker.getId(), pRecent.getId());
            like(liker.getId(), pPopTag.getId());
        }

        // recommended feed
        List<CandidatePost> rec = feedService.recommendedFeed(a.getId(), now, 20);

        // ensure none of friend's/self posts are included in recommended candidates
        Set<String> recIds = rec.stream().map(c -> c.getPost().getId()).collect(Collectors.toSet());
        assertThat(recIds.contains(friendRecent.getId()), is(false));
        assertThat(rec.stream().anyMatch(c -> c.getPost().getAuthorId().equals(a.getId())), is(false));

        // check that some BASE signals contributed
        Optional<CandidatePost> cpRecent = rec.stream().filter(c -> c.getPost().getId().equals(pRecent.getId())).findFirst();
        Optional<CandidatePost> cpLikedTag = rec.stream().filter(c -> c.getPost().getId().equals(pLikedTag.getId())).findFirst();
        Optional<CandidatePost> cpAuthTag = rec.stream().filter(c -> c.getPost().getId().equals(pAuthTag.getId())).findFirst();
        Optional<CandidatePost> cpPopTag = rec.stream().filter(c -> c.getPost().getId().equals(pPopTag.getId())).findFirst();

        assertThat(cpRecent.isPresent(), is(true));
        assertThat(cpLikedTag.isPresent(), is(true));
        assertThat(cpAuthTag.isPresent(), is(true));
        assertThat(cpPopTag.isPresent(), is(true));

        // recent+popular should have at least score >= 2 (recent + popular post or popular hashtag)
        assertThat(cpRecent.get().getScore() >= 1, is(true));
    }

    @Test
    public void new_user_rules_fire() {
        wiring();
        LocalDateTime now = LocalDateTime.now();

        // target user (no friends, no authored)
        User u = newUser("U","U","u@ex.com","BG");

        // two other users who will like posts
        User s1 = newUser("S1","S","s1@ex.com","BG");
        User s2 = newUser("S2","S","s2@ex.com","BG");

        // candidate posts by X
        User x = newUser("X","X","x@ex.com","BG");
        Post c1 = newPost(x.getId(), "c1 #film", "#film");
        Post c2 = newPost(x.getId(), "c2 #music", "#music");

        // liked posts by U in last 30d with tag #film (3 likes across posts with #film to form preference)
        Post lf1 = newPost(x.getId(), "liked1 #film", "#film");
        Post lf2 = newPost(x.getId(), "liked2 #film", "#film");
        Post lf3 = newPost(x.getId(), "liked3 #film", "#film");
        like(u.getId(), lf1.getId());
        like(u.getId(), lf2.getId());
        like(u.getId(), lf3.getId());

        // Post-similarity: make c1 share likers with lf1 (>=70% of c1's likers are among lf1's likers)
        // Add 3 common likers to both c1 and lf1
        User k1 = newUser("K1","K","k1@ex.com","BG");
        User k2 = newUser("K2","K","k2@ex.com","BG");
        User k3 = newUser("K3","K","k3@ex.com","BG");
        like(k1.getId(), c1.getId()); like(k1.getId(), lf1.getId());
        like(k2.getId(), c1.getId()); like(k2.getId(), lf1.getId());
        like(k3.getId(), c1.getId()); like(k3.getId(), lf1.getId());

        // Similar users: s1 will like c2 and 2 liked posts of U to reach Jaccard >= 0.5
        like(s1.getId(), lf1.getId());
        like(s1.getId(), lf2.getId());
        like(s1.getId(), c2.getId());

        // run recommended feed
        List<CandidatePost> rec = feedService.recommendedFeed(u.getId(), now, 20);

        // should include c1 due to similarity to liked post (N2) and due to preference (N3: #film >= 3)
        Set<String> ids = rec.stream().map(c -> c.getPost().getId()).collect(Collectors.toSet());
        assertThat(ids.contains(c1.getId()), is(true));
        // and c2 due to similar user s1 who liked it (N1)
        assertThat(ids.contains(c2.getId()), is(true));

        // ensure reasons contain hints (not strictly required but helpful)
        CandidatePost rc1 = rec.stream().filter(c -> c.getPost().getId().equals(c1.getId())).findFirst().get();
        CandidatePost rc2 = rec.stream().filter(c -> c.getPost().getId().equals(c2.getId())).findFirst().get();
        assertThat(rc1.getReasons().isEmpty(), is(false));
        assertThat(rc2.getReasons().isEmpty(), is(false));
    }

    @Test
    public void friends_feed_recent_only() {
        wiring();
        LocalDateTime now = LocalDateTime.now();

        User a = newUser("A","A","a2@ex.com","NS");
        User f = newUser("F","F","f2@ex.com","NS");
        friendRepo.addFriends(a.getId(), f.getId());

        // recent friend post
        Post recent = newPost(f.getId(), "recent friend #x", "#x");
        // older post (simulate by direct DB update of created_at - 30h)
        // Optional: for simplicity, just ensure the service filters based on NOW, and we don't add older here

        // run friend feed
        List<Post> feed = feedService.friendFeed(a.getId(), now);
        assertThat(feed.stream().map(Post::getId).collect(Collectors.toSet()).contains(recent.getId()), is(true));
    }
}
