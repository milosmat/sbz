package model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Post {
    private final String id;
    private final String authorId;     // referenca na User.id
    private String text;
    private Set<String> hashtags = new HashSet<>();
    private int likes;
    private int reports;
    private final LocalDateTime createdAt;

    public Post(String authorId, String text, Set<String> hashtags) {
        this.id = UUID.randomUUID().toString();
        this.authorId = authorId;
        this.text = text;
        if (hashtags != null) this.hashtags = new HashSet<>(hashtags);
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public String getAuthorId() { return authorId; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Set<String> getHashtags() { return hashtags; }
    public void setHashtags(Set<String> tags) { this.hashtags = tags == null ? new HashSet<>() : new HashSet<>(tags); }
    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }
    public int getReports() { return reports; }
    public void setReports(int reports) { this.reports = reports; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
