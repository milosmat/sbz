package dto;

public class SimilarUser {
    private final String baseUserId;
    private final String otherUserId;
    private final double score;

    public SimilarUser(String baseUserId, String otherUserId, double score) {
        this.baseUserId = baseUserId; this.otherUserId = otherUserId; this.score = score;
    }
    public String getBaseUserId() { return baseUserId; }
    public String getOtherUserId() { return otherUserId; }
    public double getScore() { return score; }
}
