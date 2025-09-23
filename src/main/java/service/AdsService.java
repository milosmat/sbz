package service;

import dto.AdSuggestion;
import dto.AdsRecommendRequest;
import model.Place;
import model.Rating;
import model.User;
import model.ValidationResult;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.QueryResults;
import org.kie.api.runtime.rule.QueryResultsRow;
import repo.PlaceRepository;
import repo.PostRepository;
import repo.RatingRepository;
import repo.UserRepository;
import util.KnowledgeSessionHelper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class AdsService {
    private final UserRepository userRepo;
    private final PostRepository postRepo;
    private final PlaceRepository placeRepo;
    private final RatingRepository ratingRepo;

    public AdsService(UserRepository userRepo, PostRepository postRepo,
                      PlaceRepository placeRepo, RatingRepository ratingRepo) {
        this.userRepo = userRepo;
        this.postRepo = postRepo;
        this.placeRepo = placeRepo;
        this.ratingRepo = ratingRepo;
    }

    // Jednostavan “par” za JDK 8 umesto record-a
    private static final class Pack {
        final String typeKw;   // npr. "bioskop", "restoran", "planina" ...
        final String topicTag; // npr. "#film", "#hrana", "#planinarenje" ...
        Pack(String typeKw, String topicTag) { this.typeKw = typeKw; this.topicTag = topicTag; }
    }

    public List<AdSuggestion> recommend(AdsRecommendRequest req, ValidationResult vr) {
        if (req == null || req.userId == null || req.userId.trim().isEmpty()) {
            vr.add("Niste ulogovani.");
            return Collections.emptyList();
        }
        if (!userRepo.existsById(req.userId)) {
            vr.add("Korisnik ne postoji.");
            return Collections.emptyList();
        }

        // Grad korisnika (za DRL filter)
        String userCity = "";
        try {
            Optional<User> u = userRepo.findById(req.userId);
            if (u.isPresent() && u.get().getCity() != null) {
                userCity = u.get().getCity();
            }
        } catch (Exception ignore) {}

        KieContainer kc = KnowledgeSessionHelper.createRuleBase();
        KieSession ksession = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "ads-session");

        try {
            // Globals
            ksession.setGlobal("userRepo", userRepo);
            ksession.setGlobal("postRepo", postRepo);
            ksession.setGlobal("placeRepo", placeRepo);
            ksession.setGlobal("ratingRepo", ratingRepo);
            ksession.setGlobal("NOW", LocalDateTime.now());

            // Činjenice u WM: sva mesta, sve ocene korisnika, relevantni postovi
            for (Place p : placeRepo.findAll()) {
                ksession.insert(p);
            }
            for (Rating r : ratingRepo.findByUser(req.userId)) {
                ksession.insert(r);
            }
            // Učitaj postove iz poslednjih 30 dana (po potrebi promeni period)
            LocalDateTime since = LocalDateTime.now().minusDays(30);
            for (model.Post p : postRepo.findSince(since)) {
                ksession.insert(p);
            }

            // Podesivi set “paketa” (tip + tematski hashtag)
            List<Pack> packs = Arrays.asList(
                new Pack("bioskop",  "#film"),
                new Pack("restoran", "#hrana"),
                new Pack("planina",  "#planinarenje"),
                new Pack("more",     "#letovanje"),
                new Pack("obala",    "#plaza")
            );

            Set<String> seenPlaceIds = new HashSet<String>();
            List<AdSuggestion> out = new ArrayList<AdSuggestion>();

            // Pozovi generički backward-chaining query za svaki pack
            for (Pack p : packs) {
                QueryResults qr = ksession.getQueryResults(
                        "Ads:ForTypeAndTag",
                        req.userId,
                        userCity == null ? "" : userCity,
                        p.typeKw,
                        p.topicTag
                );
                for (QueryResultsRow row : qr) {
                    Place place = (Place) row.get("$p");
                    String why  = (String) row.get("$why");
                    if (place != null && seenPlaceIds.add(place.getId())) {
                        out.add(new AdSuggestion(place, why));
                    }
                }
            }

            Collections.shuffle(out);

            int limit = req.limit <= 0 ? 10 : req.limit;
            return out.stream().limit(limit).collect(Collectors.toList());
        } finally {
            ksession.dispose();
        }
    }
}
