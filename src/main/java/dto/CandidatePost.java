package dto;

import model.Post;
import java.util.ArrayList;
import java.util.List;

public class CandidatePost {
	private final Post post;
	private int score = 0;
	private final List<String> reasons = new ArrayList<>();


	public CandidatePost(Post post) { this.post = post; }


	public Post getPost() { return post; }
	public int getScore() { return score; }
	public List<String> getReasons() { return reasons; }


	public void addScore(int a, String reason) {
	this.score += a;
	if (reason != null) reasons.add(reason);
	}
}
