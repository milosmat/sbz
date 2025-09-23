// src/main/java/dto/RecommendedFeedRequest.java
package dto;

public class RecommendedFeedRequest {
    public final String userId;
    public RecommendedFeedRequest(String userId) { this.userId = userId; }
    public String getUserId() { return userId; }  // <— DODATO
}
