package service;

import dto.CreateRatingRequest;
import model.Rating;
import model.ValidationResult;
import repo.PlaceRepository;
import repo.RatingRepository;
import repo.UserRepository;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class RatingService {
    private final RatingRepository ratingRepo;
    private final UserRepository userRepo;
    private final PlaceRepository placeRepo;

    public RatingService(RatingRepository ratingRepo, UserRepository userRepo, PlaceRepository placeRepo) {
        this.ratingRepo = ratingRepo;
        this.userRepo = userRepo;
        this.placeRepo = placeRepo;
    }

    public Rating create(CreateRatingRequest req, ValidationResult vr) {
        if (req == null) { vr.add("Prazan zahtev."); return null; }
        if (req.userId == null || req.userId.trim().isEmpty()) vr.add("Niste ulogovani.");
        if (req.placeId == null || req.placeId.trim().isEmpty()) vr.add("Mesto je obavezno.");
        if (req.score < 1 || req.score > 5) vr.add("Ocena mora biti 1-5.");
        if (!userRepo.existsById(req.userId)) vr.add("Korisnik ne postoji.");
        if (!placeRepo.existsById(req.placeId)) vr.add("Mesto ne postoji.");
        if (!vr.isOk()) return null;

        Set<String> tags = parseHashtags(req.hashtagsLine);
        Rating r = new Rating(req.userId, req.placeId, req.score, req.description, tags, LocalDateTime.now());
        return ratingRepo.save(r);
    }

    private Set<String> parseHashtags(String line) {
        Set<String> out = new HashSet<>();
        if (line == null) return out;
        for (String t : line.split("[\\s,;]+")) {
            if (t != null && t.startsWith("#") && t.length() > 1) out.add(t.toLowerCase());
        }
        return out;
    }
}
