package service;

import dto.CreatePlaceRequest;
import model.Place;
import model.ValidationResult;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import repo.PlaceRepository;
import repo.UserRepository;
import util.KnowledgeSessionHelper;

import java.util.HashSet;
import java.util.Set;

public class PlaceService {
    private final PlaceRepository placeRepo;
    private final UserRepository userRepo;

    public PlaceService(PlaceRepository placeRepo, UserRepository userRepo) {
        this.placeRepo = placeRepo;
        this.userRepo = userRepo;
    }

    public Place createPlace(CreatePlaceRequest req) {
        ValidationResult vr = new ValidationResult();

        KieContainer kc = KnowledgeSessionHelper.createRuleBase();
        KieSession ks = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            ks.setGlobal("userRepo", userRepo);
            ks.setGlobal("placeRepo", placeRepo);

            ks.insert(req);
            ks.insert(vr);

            ks.getAgenda().getAgendaGroup("place-add").setFocus();
            ks.fireAllRules();
        } finally {
            ks.dispose();
        }

        if (!vr.isOk()) {
            throw new IllegalArgumentException(java.lang.String.join("; ", vr.getErrors()));
        }

        Set<String> tags = parseHashtags(req.hashtagsLine);
        Place p = new Place(req.name, req.country, req.city, req.description, tags);
        return placeRepo.save(p);
    }   
    
    private Set<String> parseHashtags(String line) {
        Set<String> out = new HashSet<String>();
        if (line == null) return out;
        String[] parts = line.split("[\\s,;]+");
        for (String t : parts) {
            if (t != null && t.startsWith("#") && t.length() > 1) {
                out.add(t.toLowerCase());
            }
        }
        return out;
    }
}
