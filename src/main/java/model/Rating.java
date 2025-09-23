package model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Rating {
    private final String id;
    private final String userId;
    private final String placeId;
    private final int score; // 1-5
    private final String description;
    private final Set<String> hashtags = new HashSet<>();
    private final LocalDateTime createdAt;

    public Rating(String userId, String placeId, int score, String description, Set<String> hashtags, LocalDateTime createdAt) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.placeId = placeId;
        this.score = score;
        this.description = description == null ? "" : description;
        if (hashtags != null) this.hashtags.addAll(hashtags);
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public String getPlaceId() { return placeId; }
    public int getScore() { return score; }
    public String getDescription() { return description; }
    public Set<String> getHashtags() { return hashtags; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
