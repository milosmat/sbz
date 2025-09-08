package model;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Place {
    private String id;
    private String name;      // Naziv
    private String country;   // Država
    private String city;      // Grad
    private String description;
    private Set<String> hashtags = new HashSet<String>();

    public Place(String name, String country, String city, String description, Set<String> hashtags) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.country = country;
        this.city = city;
        this.description = description;
        if (hashtags != null) this.hashtags.addAll(hashtags);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCountry() { return country; }
    public String getCity() { return city; }
    public String getDescription() { return description; }
    public Set<String> getHashtags() { return hashtags; }

    public void setDescription(String description) { this.description = description; }
}
