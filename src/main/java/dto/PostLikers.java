package dto;

import java.util.Set;

public class PostLikers {
    private final String postId;
    private final java.util.Set<String> userIds;
    public PostLikers(String postId, Set<String> userIds){ this.postId = postId; this.userIds = userIds; }
    public String getPostId(){ return postId; }
    public Set<String> getUserIds(){ return userIds; }
}
