package dto;

public class FriendSearchRequest {
    public String userId; // ulogovani
    public String query;  // pojam
    public FriendSearchRequest(String userId, String query) {
        this.userId = userId; this.query = query;
    }
}
