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

// DEBUG listeners
import org.kie.api.event.rule.*;
import org.kie.api.runtime.rule.FactHandle;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

public class FeedService {

    private static final boolean DEBUG = true; // ⇐ upali/ugasi debug
    private static void log(String msg){ if (DEBUG) System.out.println("[FEED] " + msg); }

    private final UserRepository userRepo;
    private final FriendRepository friendRepo;
    private final PostRepository postRepo;

    public FeedService(UserRepository userRepo, FriendRepository friendRepo, PostRepository postRepo) {
        this.userRepo = userRepo;
        this.friendRepo = friendRepo;
        this.postRepo = postRepo;
    }

    // ===== internal: attach Drools listeners =====
    private void attachDebugListeners(KieSession ks, String tag){
        if (!DEBUG) return;

        ks.addEventListener(new AgendaEventListener() {
            @Override public void matchCreated(MatchCreatedEvent event) {
                log(tag + " MATCH + " + event.getMatch().getRule().getName());
            }
            @Override public void matchCancelled(MatchCancelledEvent event) {
                log(tag + " MATCH - " + event.getMatch().getRule().getName());
            }
            @Override public void beforeMatchFired(BeforeMatchFiredEvent event) {
                log(tag + " FIRE --> " + event.getMatch().getRule().getName());
            }
            @Override public void afterMatchFired(AfterMatchFiredEvent event) {
                log(tag + " FIRE <-- " + event.getMatch().getRule().getName());
            }
            @Override public void agendaGroupPopped(AgendaGroupPoppedEvent event) {
                log(tag + " POP  [" + event.getAgendaGroup().getName() + "]");
            }
            @Override public void agendaGroupPushed(AgendaGroupPushedEvent event) {
                log(tag + " PUSH [" + event.getAgendaGroup().getName() + "]");
            }
            @Override public void beforeRuleFlowGroupActivated(RuleFlowGroupActivatedEvent e) { }
            @Override public void afterRuleFlowGroupActivated(RuleFlowGroupActivatedEvent e) { }
            @Override public void beforeRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent e) { }
            @Override public void afterRuleFlowGroupDeactivated(RuleFlowGroupDeactivatedEvent e) { }
        });

        ks.addEventListener(new RuleRuntimeEventListener() {
            @Override public void objectInserted(ObjectInsertedEvent event) {
                Object o = event.getObject();
                log(tag + " INSERT " + shortOf(o));
            }
            @Override public void objectUpdated(ObjectUpdatedEvent event) {
                Object o = event.getObject();
                log(tag + " UPDATE " + shortOf(o));
            }
            @Override public void objectDeleted(ObjectDeletedEvent event) {
                Object o = event.getOldObject();
                log(tag + " DELETE " + shortOf(o));
            }
        });
    }

    private String shortOf(Object o){
        if (o == null) return "null";
        String cn = o.getClass().getSimpleName();
        if (o instanceof Post){
            Post p = (Post)o;
            return cn+"{id="+p.getId()+", likes="+p.getLikes()+", ts="+p.getCreatedAt()+"}";
        }
        if (o instanceof CandidatePost){
            CandidatePost c = (CandidatePost)o;
            String id = c.getPost()==null? "null": c.getPost().getId();
            return cn+"{postId="+id+", score="+c.getScore()+", reasons="+c.getReasons()+"}";
        }
        if (o instanceof UserFeedContext){
            UserFeedContext u = (UserFeedContext)o;
            return cn+"{uid="+u.getUserId()+", liked="+u.getLikedHashtags()+", authored="+u.getAuthoredHashtags()+"}";
        }
        if (o instanceof FriendIds){
            return cn+"{size="+((FriendIds)o).getIds().size()+"}";
        }
        if (o instanceof BlockedIds){
            return cn+"{size="+((BlockedIds)o).getIds().size()+"}";
        }
        if (o instanceof PopularHashtag){
            return cn+"{tag="+((PopularHashtag)o).getTag()+"}";
        }
        if (o instanceof PopularPost){
            return cn+"{postId="+((PopularPost)o).getPostId()+"}";
        }
        if (o instanceof FriendFeedRequest){
            return cn+"{userId="+((FriendFeedRequest)o).getUserId()+"}";
        }
        if (o instanceof RecommendedFeedRequest){
            return cn+"{userId="+((RecommendedFeedRequest)o).getUserId()+"}";
        }
        if (o instanceof UserAuthoredCount){
            return cn+"{uid="+((UserAuthoredCount)o).getUserId()+", count="+((UserAuthoredCount)o).getCount()+"}";
        }
        return cn;
    }

    // FRIENDS feed
    public List<Post> friendFeed(String userId, LocalDateTime now) {
        ValidationResult vr = new ValidationResult();
        KieContainer kc = KnowledgeSessionHelper.createRuleBase();
        KieSession ks = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        attachDebugListeners(ks, "[FRIENDS]");

        try {
            ks.setGlobal("userRepo", userRepo);
            ks.setGlobal("NOW", now);

            ks.insert(new FriendFeedRequest(userId));
            ks.insert(vr);
            ks.getAgenda().getAgendaGroup("feed-friends-validate").setFocus();
            ks.getAgenda().getAgendaGroup("feed-friends-select").setFocus();

            Set<String> friends = friendRepo.getFriendsOf(userId);
            Set<String> blocked = friendRepo.getBlockedBy(userId);
            log("friendsFeed: friends=" + friends.size() + " blocked=" + blocked.size());
            ks.insert(new FriendIds(friends));
            ks.insert(new BlockedIds(blocked));

            List<Post> pool = postRepo.findSince(now.minusHours(48));
            log("friendsFeed: pool posts (48h) = " + pool.size());
            pool.stream().filter(Objects::nonNull).forEach(ks::insert);

            List<Post> out = new ArrayList<>();
            ks.setGlobal("friendsOut", out);

            int fired = ks.fireAllRules();
            log("friendsFeed: rules fired = " + fired);

            if (!vr.isOk()) throw new IllegalArgumentException(String.join("; ", vr.getErrors()));

            List<Post> result = out.stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(Post::getCreatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                    .collect(Collectors.toList());

            log("friendsFeed: result size = " + result.size());
            return result;
        } finally { ks.dispose(); }
    }

    // RECOMMENDED feed
    public List<CandidatePost> recommendedFeed(String userId, LocalDateTime now, int limit) {
        ValidationResult vr = new ValidationResult();
        KieContainer kc = KnowledgeSessionHelper.createRuleBase();
        KieSession ks = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        attachDebugListeners(ks, "[RECO]");

        try {
            ks.setGlobal("userRepo", userRepo);
            ks.setGlobal("NOW", now);

            ks.insert(new RecommendedFeedRequest(userId));
            ks.insert(vr);

            ks.getAgenda().getAgendaGroup("feed-recommend-score").setFocus();
            ks.getAgenda().getAgendaGroup("feed-recommend-router").setFocus();
            ks.getAgenda().getAgendaGroup("feed-recommend-validate").setFocus();

            Set<String> friends = friendRepo.getFriendsOf(userId);
            int authoredCount = postRepo.countByAuthor(userId);
            Set<String> likedTags    = postRepo.findUserLikedHashtags(userId, now.minusDays(3));
            Set<String> authoredTags = postRepo.findUserAuthoredHashtags(userId, now.minusYears(1));

            log("reco: friends=" + friends.size() +
                " authoredCount=" + authoredCount +
                " likedTags=" + likedTags +
                " authoredTags=" + authoredTags);

            ks.insert(new FriendIds(friends));
            ks.insert(new UserAuthoredCount(userId, authoredCount));
            ks.insert(new UserFeedContext(userId, now, likedTags, authoredTags));

            // popular (24h)
            LocalDateTime last24h = now.minusHours(24);
            Map<String,Integer> ht24 = postRepo.countHashtagUsageSince(last24h);
            long popTagCount = ht24.entrySet().stream().filter(e -> e.getValue() > 5).count();
            ht24.entrySet().stream().filter(e -> e.getValue() > 5).forEach(e -> ks.insert(new PopularHashtag(e.getKey())));
            log("reco: popular hashtags (24h) = " + popTagCount);

            // pool 7d
            List<Post> pool = postRepo.findSince(now.minusDays(7));
            log("reco: pool posts (7d) = " + pool.size());
            for (Post p : pool) {
                if (p == null) continue;
                ks.insert(p);
                if (p.getCreatedAt() != null && p.getCreatedAt().isAfter(last24h) && p.getLikes() > 10) {
                    ks.insert(new PopularPost(p.getId()));
                }
                ks.insert(new CandidatePost(p));
            }

            int fired = ks.fireAllRules();
            log("reco: rules fired = " + fired);

            if (!vr.isOk()) throw new IllegalArgumentException(String.join("; ", vr.getErrors()));

            // pokupi kandidate iz WM
            List<CandidatePost> recs = ks.getObjects(o -> o instanceof CandidatePost)
                    .stream().map(o -> (CandidatePost) o)
                    .filter(Objects::nonNull)
                    .filter(c -> c.getPost() != null)
                    .sorted(Comparator
                        .comparingInt(CandidatePost::getScore).reversed()
                        .thenComparing(cp -> cp.getPost().getCreatedAt(),
                                Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(Math.max(1, limit))
                    .collect(Collectors.toList());

            // ispiši rezultate (score + reasons)
            for (CandidatePost c : recs) {
                String id = c.getPost()==null ? "null" : c.getPost().getId();
                log("reco OUT: postId=" + id + " score=" + c.getScore() + " reasons=" + c.getReasons());
            }

            return recs;
        } finally { ks.dispose(); }
    }
}
