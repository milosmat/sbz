package dto;

public class CreateRatingRequest {
    public String userId;
    public String placeId;
    public int score; // 1-5
    public String description;
    public String hashtagsLine; // npr. "#film #romantika"

    public CreateRatingRequest() {}

    public CreateRatingRequest(String userId, String placeId, int score, String description, String hashtagsLine) {
        this.userId = userId;
        this.placeId = placeId;
        this.score = score;
        this.description = description;
        this.hashtagsLine = hashtagsLine;
    }
}
