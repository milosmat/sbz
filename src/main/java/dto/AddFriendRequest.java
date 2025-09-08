package dto;

public class AddFriendRequest {
    public String userId;   // ko dodaje
    public String targetId; // koga dodaje

    public AddFriendRequest(String userId, String targetId) {
        this.userId = userId;
        this.targetId = targetId;
    }
}
