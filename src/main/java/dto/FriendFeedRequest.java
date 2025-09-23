// src/main/java/dto/FriendFeedRequest.java
package dto;

public class FriendFeedRequest {
    public final String userId;
    public FriendFeedRequest(String userId) { this.userId = userId; }
    public String getUserId() { return userId; }  // <— DODATO
}
