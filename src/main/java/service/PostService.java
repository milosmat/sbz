package service;

import dto.CreatePostRequest;
import dto.LikePostRequest;
import dto.MyPostsRequest;
import dto.ReportPostRequest;
import model.Post;
import model.ValidationResult;
import repo.PostRepository;
import repo.UserRepository;
import util.KnowledgeSessionHelper;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PostService {
    private final PostRepository postRepo;
    private final UserRepository userRepo;

    public PostService(PostRepository postRepo, UserRepository userRepo) {
        this.postRepo = postRepo;
        this.userRepo = userRepo;
    }

    public List<Post> listMyPosts(String userId) {
        // 1) validacije kroz Drools (agenda-group "my-posts")
        ValidationResult vr = new ValidationResult();
        KieContainer kc = KnowledgeSessionHelper.createRuleBase();
        KieSession ksession = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");

        try {
            ksession.setGlobal("userRepo", userRepo);
            ksession.insert(new MyPostsRequest(userId));
            ksession.insert(vr);

            ksession.getAgenda().getAgendaGroup("my-posts").setFocus();
            ksession.fireAllRules();
        } finally {
            ksession.dispose();
        }

        if (!vr.isOk()) {
            throw new IllegalArgumentException(String.join("; ", vr.getErrors()));
        }

        // 2) ako je validno, vrati objave (sort: najnovije prve)
        return postRepo.findByAuthor(userId);
    }
    
    public Post createPost(CreatePostRequest req) {
    	
        if (userRepo.isPostingSuspended(req.authorId)) {
            long until = userRepo.postingSuspendedUntil(req.authorId);
            throw new IllegalArgumentException("Zabranjeno objavljivanje do " + new java.util.Date(until));
        }
        
        ValidationResult vr = new ValidationResult();

        KieContainer kc = KnowledgeSessionHelper.createRuleBase();
        KieSession ksession = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            ksession.setGlobal("userRepo", userRepo);
            ksession.insert(req);
            ksession.insert(vr);
            ksession.getAgenda().getAgendaGroup("post-create").setFocus();
            ksession.fireAllRules();
        } finally {
            ksession.dispose();
        }
        
        if (!vr.isOk()) throw new IllegalArgumentException(String.join("; ", vr.getErrors()));

        Set<String> tags = parseHashtags(req.hashtagsLine);
        Post p = new Post(req.authorId, req.text, tags);
        return postRepo.save(p);
    }

    private Set<String> parseHashtags(String line) {
        Set<String> out = new HashSet<>();
        if (line == null) return out;
        String[] parts = line.split("[\\s,;]+");
        for (String t : parts) {
            if (t != null && t.startsWith("#") && t.length() > 1) {
                out.add(t.toLowerCase());
            }
        }
        return out;
    }
    
    public Post likePost(String userId, String postId) {
        ValidationResult vr = new ValidationResult();

        KieContainer kc = KnowledgeSessionHelper.createRuleBase();
        KieSession ksession = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            ksession.setGlobal("userRepo", userRepo);
            ksession.setGlobal("postRepo", postRepo);

            ksession.insert(new LikePostRequest(userId, postId));
            ksession.insert(vr);

            ksession.getAgenda().getAgendaGroup("post-like").setFocus();
            ksession.fireAllRules();
        } finally {
            ksession.dispose();
        }

        if (!vr.isOk()) {
            if (vr.getErrors().size() == 1
                && "Već ste lajkovali ovu objavu.".equals(vr.getErrors().get(0))) {
                return postRepo.findById(postId)
                        .orElseThrow(() -> new IllegalArgumentException("Objava ne postoji."));
            }
            throw new IllegalArgumentException(String.join("; ", vr.getErrors()));
        }


        // prvi like – stvarno upiši
        return postRepo.like(postId, userId);
    }
    
    public Post reportPost(String userId, String postId, String reason) {
        ValidationResult vr = new ValidationResult();

        KieContainer kc = KnowledgeSessionHelper.createRuleBase();
        KieSession ks = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            ks.setGlobal("userRepo", userRepo);
            ks.setGlobal("postRepo", postRepo);

            ks.insert(new ReportPostRequest(userId, postId, reason));
            ks.insert(vr);

            ks.getAgenda().getAgendaGroup("post-report").setFocus();
            ks.fireAllRules();
        } finally {
            ks.dispose();
        }

        if (!vr.isOk()) {
            // idempotentno: ako je jedina greška "Već ste prijavili..." ne bacaj, samo vrati post
            if (vr.getErrors().size() == 1 && "Već ste prijavili ovu objavu.".equals(vr.getErrors().get(0))) {
                return postRepo.findById(postId)
                        .orElseThrow(() -> new IllegalArgumentException("Objava ne postoji."));
            }
            throw new IllegalArgumentException(String.join("; ", vr.getErrors()));
        }
        Post p = postRepo.report(postId, userId);
        
        repo.ModerationEventsRepository.getInstance().recordReport(p.getAuthorId(), userId, postId);
	    return p;
    }

}
