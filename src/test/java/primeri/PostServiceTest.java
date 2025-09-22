package primeri;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.Connection;
import java.sql.Statement;
import java.util.*;

import model.Post;
import model.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import db.Db;
import dto.CreatePostRequest;
import repo.PostRepository;
import repo.UserRepository;
import service.PostService;

public class PostServiceTest {

    @Before
    @After
    public void cleanupbefore() throws Exception {
        try (Connection c = Db.get(); Statement st = c.createStatement()) {
            st.executeUpdate("TRUNCATE users CASCADE");
            st.executeUpdate("TRUNCATE posts CASCADE");
        }
    }
    
    @Test
    public void listanjeMojihObjava_sortiraPoNovijim() throws InterruptedException {
        UserRepository userRepo = new UserRepository();
        PostRepository postRepo = new PostRepository();
        PostService postService = new PostService(postRepo, userRepo);

        User u = new User("Ana","Anić","ana@example.com","hash","Kragujevac");
        userRepo.save(u);
        Set<String> java = new HashSet<>(Arrays.asList("#java"));
        Set<String> sbz = new HashSet<>(Arrays.asList("#sbz"));
        Post p1 = new Post(u.getId(), "Prva", sbz);
        postRepo.save(p1);
        Thread.sleep(5); // da bi createdAt bio različit
        Post p2 = new Post(u.getId(), "Druga", java);
        postRepo.save(p2);

        List<Post> list = postService.listMyPosts(u.getId());
        assertThat(list.size(), is(2));
        assertThat(list.get(0).getText(), is("Druga")); // novija prva
        assertThat(list.get(1).getText(), is("Prva"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void neulogovan_baca_izuzetak() {
        UserRepository userRepo = new UserRepository();
        PostRepository postRepo = new PostRepository();
        PostService postService = new PostService(postRepo, userRepo);

        postService.listMyPosts(""); // my-posts pravilo će da odbije
    }
    
    @Test
    public void kreiranje_post_sa_hashtags() {
        UserRepository userRepo = new UserRepository();
        PostRepository postRepo = new PostRepository();
        PostService postService = new PostService(postRepo, userRepo);

        User u = new User("Ana","Anić","ana@example.com","hash","Kragujevac");
        userRepo.save(u);

        Post p = postService.createPost(new CreatePostRequest(u.getId(), "Hello", "#sbz #Java #java"));
        assertThat(p.getText(), is("Hello"));
        // duplikat #java se uklanja u Set-u; sve je lowercase
        assertThat(p.getHashtags().contains("#sbz"), is(true));
        assertThat(p.getHashtags().contains("#java"), is(true));
    }
    
    @Test
    public void like_povecava_broj_lajkova_i_ne_duplira() {
        UserRepository ur = new UserRepository();
        PostRepository pr = new PostRepository();
        PostService svc = new PostService(pr, ur);

        User u = new User("Pera","Peric","p@e.com","hash","BG");
        ur.save(u);

        Post p = new Post(u.getId(), "Tekst", Collections.<String>emptySet());
        pr.save(p);

        Post p1 = svc.likePost(u.getId(), p.getId());
        assertThat(p1.getLikes(), is(1));

        Post p2 = svc.likePost(u.getId(), p.getId()); // drugi put isti korisnik
        assertThat(p2.getLikes(), is(1)); // ne raste
    }
    
    @Test
    public void report_povecava_broj_i_ne_duplira() {
        UserRepository ur = new UserRepository();
        PostRepository pr = new PostRepository();
        PostService svc = new PostService(pr, ur);

        User author = new User("A","B","a@b.com","h","C"); ur.save(author);
        User reporter = new User("R","R","r@r.com","h","C"); ur.save(reporter);

        Post p = new Post(author.getId(), "txt", Collections.<String>emptySet());
        pr.save(p);

        Post p1 = svc.reportPost(reporter.getId(), p.getId(), "spam");
        assertThat(p1.getReports(), is(1));

        // drugi put isti korisnik -> idempotentno (broj ostaje isti, bez izuzetka)
        Post p2 = svc.reportPost(reporter.getId(), p.getId(), "spam");
        assertThat(p2.getReports(), is(1));
    }
}
