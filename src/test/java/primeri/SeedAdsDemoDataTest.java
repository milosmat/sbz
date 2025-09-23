package primeri;

import org.junit.Test;

import dto.RegisterRequest;
import dto.CreatePlaceRequest;
import dto.CreatePostRequest;
import dto.CreateRatingRequest;

import model.Place;
import model.User;
import model.ValidationResult;

import repo.PlaceRepository;
import repo.PostRepository;
import repo.RatingRepository;
import repo.UserRepository;

import service.PlaceService;
import service.PostService;
import service.RatingService;
import service.RegistrationService;

import java.util.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

public class SeedAdsDemoDataTest {

  @Test
  public void seed_demo_user_and_ads_data() {
    // Repozitorijumi i servisi
    UserRepository ur = new UserRepository();
    PostRepository pr = new PostRepository();
    PlaceRepository plr = new PlaceRepository();
    RatingRepository rr = new RatingRepository();

    RegistrationService reg = new RegistrationService(ur);
    PostService post = new PostService(pr, ur);
    PlaceService place = new PlaceService(plr, ur);
    RatingService rate = new RatingService(rr, ur, plr);

    // ===== ADMIN (mora biti admin zbog kreiranja mesta) =====
    User admin = ur.findByEmail("admin@sbz.com")
        .orElseGet(() -> reg.register(new RegisterRequest("Admin","Admin","admin@sbz.com","123456","Novi Sad")));
    ur.markAsAdmin(admin.getId()); // obavezno!

    // ===== KORISNICI =====
    User pera = ur.findByEmail("pera@example.com")
        .orElseGet(() -> reg.register(new RegisterRequest("Pera","Peric","pera@example.com","lozinka","Novi Sad")));
    User mika = ur.findByEmail("mika@example.com")
        .orElseGet(() -> reg.register(new RegisterRequest("Mika","Mikic","mika@example.com","lozinka","Beograd")));
    User joca = ur.findByEmail("joca@example.com")
        .orElseGet(() -> reg.register(new RegisterRequest("Joca","Jovic","joca@example.com","lozinka","Zlatibor")));
    User luka = ur.findByEmail("luka@example.com")
        .orElseGet(() -> reg.register(new RegisterRequest("Luka","Lukic","luka@example.com","lozinka","Beograd")));

    // ===== HELPERI =====
    // Bez dupliranja mesta: ako već postoji (name, city), samo ga vrati iz repozitorijuma
    java.util.function.Function<CreatePlaceRequest, Place> ensurePlace = (req) -> {
      if (plr.existsByNameAndCity(req.name, req.city)) {
        return plr.findAll().stream()
            .filter(p -> p.getName().equalsIgnoreCase(req.name) && p.getCity().equalsIgnoreCase(req.city))
            .findFirst().orElseGet(() -> place.createPlace(req));
      }
      return place.createPlace(req);
    };

    java.util.function.BiConsumer<String, String> likePost = (userId, postId) -> post.likePost(userId, postId);

    java.util.function.BiConsumer<String, String> mkAndLike = (userId, tag) -> {
      // 4 posta sa datim tagom i lajkovi istog korisnika ( > 3 )
      String p0 = post.createPost(new CreatePostRequest(userId, tag + " objava 0", tag)).getId();
      String p1 = post.createPost(new CreatePostRequest(userId, tag + " objava 1", tag)).getId();
      String p2 = post.createPost(new CreatePostRequest(userId, tag + " objava 2", tag)).getId();
      String p3 = post.createPost(new CreatePostRequest(userId, tag + " objava 3", tag)).getId();
      likePost.accept(userId, p0);
      likePost.accept(userId, p1);
      likePost.accept(userId, p2);
      likePost.accept(userId, p3);
    };

    java.util.function.Consumer<ValidationResult> assertOk = (vr) -> {
      if (!vr.isOk()) throw new RuntimeException(String.join("; ", vr.getErrors()));
    };

    // ===== SCENARIO 1: #bioskop / #film za PERU (Novi Sad) =====
    Place nsCandidateCinema = ensurePlace.apply(new CreatePlaceRequest(
        admin.getId(), "Arena Cineplex", "Srbija", "Novi Sad",
        "Veliki multipleks bioskop sa #film repertoarom", "#bioskop #film"));

    Place nsVisitedCinema = ensurePlace.apply(new CreatePlaceRequest(
        admin.getId(), "Bioskop CineStar", "Srbija", "Novi Sad",
        "Drugi bioskop", "#bioskop"));

    // pozitivna ocena tipa #bioskop
    ValidationResult vr1 = new ValidationResult();
    rate.create(new CreateRatingRequest() {{
      userId = pera.getId();
      placeId = nsVisitedCinema.getId();
      score = 5;
      description = "Super bioskop!";
      hashtagsLine = "#bioskop";
    }}, vr1);
    assertOk.accept(vr1);

    // sopstveni postovi i lajkovi sa #film ( > 3 )
    mkAndLike.accept(pera.getId(), "#film");

    // ===== SCENARIO 2: #restoran / #hrana za MIKU (Beograd) =====
    Place bgCandidateRestaurant = ensurePlace.apply(new CreatePlaceRequest(
        admin.getId(), "Tri Šešira", "Srbija", "Beograd",
        "Tradicionalni restoran u Skadarliji", "#restoran #hrana"));

    Place bgVisitedRestaurant = ensurePlace.apply(new CreatePlaceRequest(
        admin.getId(), "Manufaktura", "Srbija", "Beograd",
        "Restoran", "#restoran"));

    ValidationResult vr2 = new ValidationResult();
    rate.create(new CreateRatingRequest() {{
      userId = mika.getId();
      placeId = bgVisitedRestaurant.getId();
      score = 4;                // ≥4
      description = "Dobra klopa!";
      hashtagsLine = "#restoran";
    }}, vr2);
    assertOk.accept(vr2);

    mkAndLike.accept(mika.getId(), "#hrana");

    // ===== SCENARIO 3: #planina / #planinarenje za JOCU (Zlatibor) =====
    Place zlCandidateMountain = ensurePlace.apply(new CreatePlaceRequest(
        admin.getId(), "Tornik", "Srbija", "Zlatibor",
        "Planinski vrh i staze, penjanje i #planinarenje", "#planina #planinarenje"));

    Place zlVisitedMountain = ensurePlace.apply(new CreatePlaceRequest(
        admin.getId(), "Čigota", "Srbija", "Zlatibor",
        "Planinski dom", "#planina"));

    ValidationResult vr3 = new ValidationResult();
    rate.create(new CreateRatingRequest() {{
      userId = joca.getId();
      placeId = zlVisitedMountain.getId();
      score = 5;
      description = "Odlična staza!";
      hashtagsLine = "#planina";
    }}, vr3);
    assertOk.accept(vr3);

    mkAndLike.accept(joca.getId(), "#planinarenje");

    // ===== SCENARIO 4: “Multi” mesto (više tema) za LUKU (Beograd) =====
    Place bgMulti = ensurePlace.apply(new CreatePlaceRequest(
        admin.getId(), "BIG Mall", "Srbija", "Beograd",
        "Kompleks sa bioskopom i restoranima, za #film i #hrana", "#bioskop #film #restoran #hrana"));

    // ocene tipova koje “otključavaju” i bioskop i restoran
    Place bgAnyCinema = ensurePlace.apply(new CreatePlaceRequest(
        admin.getId(), "Any Cinema BG", "Srbija", "Beograd", "bioskop", "#bioskop"));
    Place bgAnyRest = ensurePlace.apply(new CreatePlaceRequest(
        admin.getId(), "Any Rest BG", "Srbija", "Beograd", "restoran", "#restoran"));

    ValidationResult vr4a = new ValidationResult();
    rate.create(new CreateRatingRequest() {{
      userId = luka.getId();
      placeId = bgAnyCinema.getId();
      score = 5;
      description = "Kul bioskop.";
      hashtagsLine = "#bioskop";
    }}, vr4a); assertOk.accept(vr4a);

    ValidationResult vr4b = new ValidationResult();
    rate.create(new CreateRatingRequest() {{
      userId = luka.getId();
      placeId = bgAnyRest.getId();
      score = 4;
      description = "Ok restoran.";
      hashtagsLine = "#restoran";
    }}, vr4b); assertOk.accept(vr4b);

    // Luka koristi i lajkuje oba taga
    mkAndLike.accept(luka.getId(), "#film");
    mkAndLike.accept(luka.getId(), "#hrana");

    // Završne sanity provere za seed (nije obavezno, ali lepo je imati)
    assertThat("admin je admin", ur.isAdmin(admin.getId()), is(true));
  }
}
