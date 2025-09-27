package dto;

public class UserPreferredTag {
    private final String userId;
    private final String tag;
    private final int count;
    public UserPreferredTag(String userId, String tag, int count){ this.userId=userId; this.tag=tag; this.count=count; }
    public String getUserId(){ return userId; }
    public String getTag(){ return tag; }
    public int getCount(){ return count; }
}
