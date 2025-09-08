package dto;

public class LikePostRequest {
    public String userId;
    public String postId;

    public LikePostRequest(String userId, String postId) {
        this.userId = userId;
        this.postId = postId;
    }
}
