package service;


import dto.*;
import model.Post;
import model.ValidationResult;
import repo.FriendRepository;
import repo.PostRepository;
import repo.UserRepository;
import util.KnowledgeSessionHelper;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;


import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class FeedService {
	
	private final UserRepository userRepo;
	private final FriendRepository friendRepo;
	private final PostRepository postRepo;
	
	public FeedService(UserRepository userRepo, FriendRepository friendRepo, PostRepository postRepo) {
		this.userRepo = userRepo;
		this.friendRepo = friendRepo;
		this.postRepo = postRepo;
	}
	
	// objave prijatelja u poslednjih 24h 
	
	public List<Post> friendFeed(String userId, LocalDateTime now) {
		ValidationResult vr = new ValidationResult();
		KieContainer kc = KnowledgeSessionHelper.createRuleBase();
		KieSession ks = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
		try {
			ks.setGlobal("userRepo", userRepo);
			ks.insert(new FriendFeedRequest(userId));
			ks.insert(vr);
			ks.getAgenda().getAgendaGroup("feed-friends").setFocus();
			ks.fireAllRules();
		} finally { ks.dispose(); }
		if (!vr.isOk()) throw new IllegalArgumentException(String.join("; ", vr.getErrors()));
	
	
		Set<String> friends = friendRepo.getFriendsOf(userId);
		if (friends.isEmpty()) return Collections.emptyList();
	
	
		LocalDateTime since = now.minusHours(24);
		List<Post> list = postRepo.findByAuthorsSince(friends, since);
	
	
		// filtriraj blokirane
		Set<String> blockedByUser = friendRepo.getBlockedBy(userId);
		return list.stream()
		.filter(p -> !blockedByUser.contains(p.getAuthorId()))
		.sorted(Comparator.comparing(Post::getCreatedAt).reversed())
		.collect(Collectors.toList());
	}
	
	// Preporuceni feed – kandidati su ne prijatelji - ocenjuje drools
	public List<CandidatePost> recommendedFeed(String userId, LocalDateTime now, int limit) {
		ValidationResult vr = new ValidationResult();
		KieContainer kc = KnowledgeSessionHelper.createRuleBase();
		KieSession ks = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
	
	
		// kandidati - poslednjih 7 dana, autori nisu prijatelji
		Set<String> friends = friendRepo.getFriendsOf(userId);
		LocalDateTime poolSince = now.minusDays(7);
		List<Post> pool = postRepo.findNonFriendPostsSince(userId, friends, poolSince);
		if (pool.isEmpty()) return Collections.emptyList();
	
	
		// lajkovani hestegovi i kreirani
		Set<String> likedTags = postRepo.findUserLikedHashtags(userId, now.minusDays(7));
		Set<String> authoredTags = postRepo.findUserAuthoredHashtags(userId, now.minusYears(1));
	
	
		// poularnost
		LocalDateTime last24h = now.minusHours(24);
		Map<String,Integer> hashtagCounts24h = postRepo.countHashtagUsageSince(last24h);
		Set<String> popularTags = hashtagCounts24h.entrySet().stream()
		.filter(e -> e.getValue() > 5)
		.map(Map.Entry::getKey)
		.collect(Collectors.toSet());
	
	
		Set<String> popularPostIds = pool.stream()
		.filter(p -> p.getCreatedAt() != null && p.getCreatedAt().isAfter(last24h) && p.getLikes() > 10)
		.map(Post::getId)
		.collect(Collectors.toSet());
	
	
		try {
			ks.setGlobal("userRepo", userRepo);
			ks.setGlobal("NOW", now);
		
		
			ks.insert(new RecommendedFeedRequest(userId));
			ks.insert(vr);
			ks.getAgenda().getAgendaGroup("feed-recommend").setFocus();
			ks.fireAllRules();
			if (!vr.isOk()) throw new IllegalArgumentException(String.join("; ", vr.getErrors()));
		
		
			// seed facts
			ks.insert(new UserFeedContext(userId, now, likedTags, authoredTags));
			for (String t : popularTags) ks.insert(new PopularHashtag(t));
			for (String pid : popularPostIds) ks.insert(new PopularPost(pid));
		
		
			List<CandidatePost> candidates = pool.stream().map(CandidatePost::new).collect(Collectors.toList());
			candidates.forEach(ks::insert);
		
		
			ks.fireAllRules();
			
			java.util.List<CandidatePost> cleaned = candidates.stream()
				    .filter(java.util.Objects::nonNull)
				    .filter(c -> c.getPost() != null)
				    .collect(java.util.stream.Collectors.toList());

				// sortiranje + limit nad 'cleaned'
				return cleaned.stream()
				    .sorted(java.util.Comparator
				        .comparingInt(CandidatePost::getScore).reversed()
				        .thenComparing(cp -> cp.getPost().getCreatedAt(),
				                       java.util.Comparator.nullsLast(java.util.Comparator.reverseOrder())))
				    .limit(Math.max(1, limit))
				    .collect(java.util.stream.Collectors.toList());
		} finally { ks.dispose(); }
	}
	
}