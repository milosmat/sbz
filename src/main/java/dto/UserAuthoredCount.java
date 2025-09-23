package dto;

public class UserAuthoredCount {
    private final String userId;
    private final int count;
    public UserAuthoredCount(String userId, int count) {
        this.userId = userId;
        this.count = count;
    }
    public String getUserId() { return userId; }
    public int getCount() { return count; }
}
