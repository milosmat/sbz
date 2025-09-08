package dto;

public class BlockUserRequest {
    public String userId;   // ko blokira
    public String targetId; // koga blokira

    public BlockUserRequest(String userId, String targetId) {
        this.userId = userId;
        this.targetId = targetId;
    }
}
