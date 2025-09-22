package dto;

import java.time.LocalDateTime;
import java.util.Set;

public class UserFeedContext {
	private final String userId;
	private final LocalDateTime now;
	private final Set<String> likedHashtags;
	private final Set<String> authoredHashtags;


	public UserFeedContext(String userId, LocalDateTime now, Set<String> likedHashtags, Set<String> authoredHashtags) {
	this.userId = userId;
	this.now = now;
	this.likedHashtags = likedHashtags;
	this.authoredHashtags = authoredHashtags;
	}
	public String getUserId() { return userId; }
	public LocalDateTime getNow() { return now; }
	public Set<String> getLikedHashtags() { return likedHashtags; }
	public Set<String> getAuthoredHashtags() { return authoredHashtags; }
}
