package model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class User {
    private final String id;
    private String firstName;
    private String lastName;
    private String email;       // jedinstven
    private String passwordHash; // čuvamo hash, ne plain tekst
    private String city;        // mesto stanovanja
    private LocalDateTime createdAt;

    public User(String firstName, String lastName, String email, String passwordHash, String city) {
        this.id = UUID.randomUUID().toString();
        this.firstName = firstName;
        this.lastName  = lastName;
        this.email     = email.toLowerCase().trim();
        this.passwordHash = passwordHash;
        this.city = city;
        this.createdAt = LocalDateTime.now();
    }

    // getteri / setteri
    public String getId() { return id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String v) { this.firstName = v; }
    public String getLastName() { return lastName; }
    public void setLastName(String v) { this.lastName = v; }
    public String getEmail() { return email; }
    public void setEmail(String v) { this.email = v.toLowerCase().trim(); }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String v) { this.passwordHash = v; }
    public String getCity() { return city; }
    public void setCity(String v) { this.city = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        return Objects.equals(id, ((User)o).id);
    }
    @Override
    public int hashCode() { return Objects.hash(id); }
}
