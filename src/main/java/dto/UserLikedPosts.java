package dto;

import java.util.Set;

public class UserLikedPosts {
    private final String userId;
    private final java.util.Set<String> postIds;
    public UserLikedPosts(String userId, Set<String> postIds){ this.userId = userId; this.postIds = postIds; }
    public String getUserId(){ return userId; }
    public Set<String> getPostIds(){ return postIds; }
}
