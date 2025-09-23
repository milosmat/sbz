package primeri;

import db.Db;
import dto.AdSuggestion;
import dto.AdsRecommendRequest;
import dto.CreatePlaceRequest;
import dto.CreatePostRequest;
import dto.CreateRatingRequest;
import dto.RegisterRequest;
import model.Place;
import model.Rating;
import model.User;
import model.ValidationResult;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import repo.PlaceRepository;
import repo.PostRepository;
import repo.RatingRepository;
import repo.UserRepository;
import service.AdsService;
import service.PlaceService;
import service.PostService;
import service.RegistrationService;
import service.RatingService;

import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class AdsServiceIntegrationTest {

    private UserRepository userRepo;
    private PostRepository postRepo;
    private PlaceRepository placeRepo;
    private RatingRepository ratingRepo;

    private RegistrationService regService;
    private PostService postService;
    private PlaceService placeService;
    private RatingService ratingService;

    @Before @After
    public void cleanupDb() throws Exception {
        try (Connection c = Db.get(); Statement st = c.createStatement()) {
            try { st.executeUpdate("TRUNCATE ratings CASCADE"); } catch (Exception ignore) {}
            try { st.executeUpdate("TRUNCATE post_likes CASCADE"); } catch (Exception ignore) {}
            try { st.executeUpdate("TRUNCATE posts CASCADE"); } catch (Exception ignore) {}
            try { st.executeUpdate("TRUNCATE places CASCADE"); } catch (Exception ignore) {}
            try { st.executeUpdate("TRUNCATE users CASCADE"); } catch (Exception ignore) {}
        }
    }

    private void wiring() {
        userRepo   = new UserRepository();
        postRepo   = new PostRepository();
        placeRepo  = new PlaceRepository();
        ratingRepo = new RatingRepository();

        regService    = new RegistrationService(userRepo);
        postService   = new PostService(postRepo, userRepo);
        placeService  = new PlaceService(placeRepo, userRepo);
        ratingService = new RatingService(ratingRepo, userRepo, placeRepo);
    }

    // ---------- helpers ----------

    private User ensureAdmin() {
        Optional<User> opt = userRepo.findByEmail("admin@sbz.com");
        if (opt.isPresent()) {
            userRepo.markAsAdmin(opt.get().getId());
            return opt.get();
        }
        User a = regService.register(new RegisterRequest("Admin","Admin","admin@sbz.com","123456","Novi Sad"));
        userRepo.markAsAdmin(a.getId());
        return a;
    }

    private User newUser(String fn, String ln, String email, String city) {
        return regService.register(new RegisterRequest(fn, ln, email, "lozinka", city));
    }

    private Place newPlace(String ownerId, String name, String country, String city, String desc, String hashtags) {
        return placeService.createPlace(new CreatePlaceRequest(ownerId, name, country, city, desc, hashtags));
    }

    private void positiveTypeRating(String userId, String placeId, int score, String typeTag) {
        CreateRatingRequest rr = new CreateRatingRequest();
        rr.userId = userId;
        rr.placeId = placeId;
        rr.score = score;
        rr.description = "poz ocena";
        rr.hashtagsLine = typeTag; // npr. "#bioskop", "#restoran"
        ValidationResult vr = new ValidationResult();
        ratingService.create(rr, vr);
        assertThat("rating valid", vr.isOk(), is(true));
    }

    private List<String> createUserPostsWithTag(String userId, String tag, int n) {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            ids.add(postService.createPost(new CreatePostRequest(userId, "post "+i+" "+tag, tag)).getId());
        }
        return ids;
    }

    private void likeN(String userId, List<String> postIds, int n) {
        for (int i = 0; i < Math.min(n, postIds.size()); i++) {
            postService.likePost(userId, postIds.get(i));
        }
    }

    private List<AdSuggestion> runAds(String userId, int limit) {
        AdsService ads = new AdsService(userRepo, postRepo, placeRepo, ratingRepo);
        AdsRecommendRequest req = new AdsRecommendRequest();
        req.userId = userId;
        req.limit = limit;
        ValidationResult vr = new ValidationResult();
        List<AdSuggestion> out = ads.recommend(req, vr);
        assertThat("validation OK", vr.isOk(), is(true));
        return out;
    }

    // ---------- tests ----------

    @Test
    public void cinema_film_happyPath() {
        wiring();
        User admin = ensureAdmin();
        User u = newUser("Pera","Peric","pera@example.com","Novi Sad");

        // kandidat (mora da ima #bioskop i #film, u istom gradu)
        Place candidate = newPlace(admin.getId(), "Arena Cineplex","Srbija","Novi Sad",
                "Veliki multipleks bioskop sa #film repertoarom", "#bioskop #film");

        // drugo bioskop mesto (za pozitivnu ocenu tipa)
        Place visited = newPlace(admin.getId(), "CineStar", "Srbija","Novi Sad",
                "Drugi bioskop", "#bioskop");
        positiveTypeRating(u.getId(), visited.getId(), 5, "#bioskop");

        // korisnik ima sopstvene postove sa #film i >3 lajkovane #film objave
        List<String> filmPosts = createUserPostsWithTag(u.getId(), "#film", 4); // sopstveni (userUsedHashtag)
        likeN(u.getId(), filmPosts, 4); // >3 lajka

        List<AdSuggestion> out = runAds(u.getId(), 10);

        assertThat(out.isEmpty(), is(false));
        assertThat(out.get(0).place.getId(), is(candidate.getId()));
        assertThat(out.get(0).why, allOf(containsString("#film"), containsString("bioskop")));
    }

    @Test
    public void restaurant_food_happyPath() {
        wiring();
        User admin = ensureAdmin();
        User u = newUser("Mika","Mikic","mika@example.com","Beograd");

        Place candidate = newPlace(admin.getId(),"Tri Šešira","Srbija","Beograd",
                "Tradicionalni restoran u Skadarliji", "#restoran #hrana");
        Place visited = newPlace(admin.getId(),"Manufaktura","Srbija","Beograd",
                "Restoran", "#restoran");

        positiveTypeRating(u.getId(), visited.getId(), 4, "#restoran"); // ≥4

        List<String> foodPosts = createUserPostsWithTag(u.getId(), "#hrana", 5);
        likeN(u.getId(), foodPosts, 5);

        List<AdSuggestion> out = runAds(u.getId(), 10);

        assertThat(out.stream().map(a -> a.place.getId()).collect(Collectors.toList()),
                hasItem(candidate.getId()));
    }

    @Test
    public void mountain_hiking_happyPath() {
        wiring();
        User admin = ensureAdmin();
        User u = newUser("Joca","Jovic","joca@example.com","Zlatibor");

        Place candidate = newPlace(admin.getId(),"Tornik","Srbija","Zlatibor",
                "Planinski vrh i staze, penjanje i #planinarenje", "#planina #planinarenje");

        Place visited = newPlace(admin.getId(),"Čigota","Srbija","Zlatibor",
                "Planinski dom", "#planina");
        positiveTypeRating(u.getId(), visited.getId(), 5, "#planina");

        List<String> hikePosts = createUserPostsWithTag(u.getId(), "#planinarenje", 4);
        likeN(u.getId(), hikePosts, 4);

        List<AdSuggestion> out = runAds(u.getId(), 10);

        assertThat(out.stream().map(a -> a.place.getId()).collect(Collectors.toList()),
                hasItem(candidate.getId()));
    }

    @Test
    public void wrong_city_excludes_candidate() {
        wiring();
        User admin = ensureAdmin();
        User u = newUser("Zika","Zikic","zika@example.com","Novi Sad");

        // kandidat u BEOGRADU (različit grad)
        Place candidate = newPlace(admin.getId(),"Arena BG","Srbija","Beograd",
                "Veliki bioskop", "#bioskop #film");

        Place visited = newPlace(admin.getId(),"NS Bioskop","Srbija","Novi Sad",
                "Bioskop NS", "#bioskop");
        positiveTypeRating(u.getId(), visited.getId(), 5, "#bioskop");

        List<String> filmPosts = createUserPostsWithTag(u.getId(), "#film", 4);
        likeN(u.getId(), filmPosts, 4);

        List<AdSuggestion> out = runAds(u.getId(), 10);

        List<String> ids = out.stream().map(a -> a.place.getId()).collect(Collectors.toList());
        assertThat(ids, not(hasItem(candidate.getId()))); // ne preporučuje zbog grada
    }

    @Test
    public void insufficient_likes_no_suggestion() {
        wiring();
        User admin = ensureAdmin();
        User u = newUser("Nina","Ninic","nina@example.com","Beograd");

        Place candidate = newPlace(admin.getId(),"Cineplexx","Srbija","Beograd",
                "Bioskop opis #film", "#bioskop #film");
        Place visited = newPlace(admin.getId(),"Kinoteka","Srbija","Beograd",
                "Stari bioskop", "#bioskop");
        positiveTypeRating(u.getId(), visited.getId(), 5, "#bioskop");

        // samo 2 lajka (potrebno >3)
        List<String> filmPosts = createUserPostsWithTag(u.getId(), "#film", 2);
        likeN(u.getId(), filmPosts, 2);

        List<AdSuggestion> out = runAds(u.getId(), 10);
        List<String> ids = out.stream().map(a -> a.place.getId()).collect(Collectors.toList());
        assertThat(ids, not(hasItem(candidate.getId())));
    }

    @Test
    public void already_rated_candidate_excluded() {
        wiring();
        User admin = ensureAdmin();
        User u = newUser("Sara","S","sara@example.com","Beograd");

        Place candidate = newPlace(admin.getId(),"XYZ Restoran","Srbija","Beograd",
                "Porodični restoran", "#restoran #hrana");

        // korisnik ocenio baš kandidata → pravilo traži da NIJE ocenio
        positiveTypeRating(u.getId(), candidate.getId(), 5, "#restoran");

        // i dalje ispunjavamo ostale uslove
        List<String> foodPosts = createUserPostsWithTag(u.getId(), "#hrana", 5);
        likeN(u.getId(), foodPosts, 5);

        List<AdSuggestion> out = runAds(u.getId(), 10);
        List<String> ids = out.stream().map(a -> a.place.getId()).collect(Collectors.toList());
        assertThat(ids, not(hasItem(candidate.getId())));
    }

    @Test
    public void dedup_and_limit() {
        wiring();
        User admin = ensureAdmin();
        User u = newUser("Luka","Lukic","luka@example.com","Beograd");

        // Mesto sa više tema (#film i #hrana) – može da ispadne kandidat kroz više pack-ova
        Place multi = newPlace(admin.getId(),"BIG Mall","Srbija","Beograd",
                "Kompleks sa bioskopom i restoranima, odličan za #film i #hrana", "#bioskop #film #restoran #hrana");

        // ocene tipova
        Place visitedCinema   = newPlace(admin.getId(),"Any Cinema","Srbija","Beograd","bioskop","#bioskop");
        Place visitedRestaurant = newPlace(admin.getId(),"Any Rest","Srbija","Beograd","restoran","#restoran");
        positiveTypeRating(u.getId(), visitedCinema.getId(), 5, "#bioskop");
        positiveTypeRating(u.getId(), visitedRestaurant.getId(), 4, "#restoran");

        // user koristi i lajkuje oba taga
        List<String> film = createUserPostsWithTag(u.getId(), "#film", 4); likeN(u.getId(), film, 4);
        List<String> food = createUserPostsWithTag(u.getId(), "#hrana", 4); likeN(u.getId(), food, 4);

        List<AdSuggestion> out = runAds(u.getId(), 1); // limit 1
        assertThat("bar 1", out.isEmpty(), is(false));
        // dedup: iako multi prođe kroz 2 pack-a, treba da se pojavi samo jednom
        assertThat(out.stream().map(a -> a.place.getId()).distinct().count(), is(1L));
    }
}
