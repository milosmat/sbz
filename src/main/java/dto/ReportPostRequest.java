package dto;

public class ReportPostRequest {
    public String userId;   // ko prijavljuje
    public String postId;   // koju objavu
    public String reason;   // opciono

    public ReportPostRequest(String userId, String postId, String reason) {
        this.userId = userId;
        this.postId = postId;
        this.reason = reason;
    }
}
