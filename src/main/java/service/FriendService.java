package service;

import dto.AddFriendRequest;
import dto.BlockUserRequest;
import model.User;
import model.ValidationResult;
import repo.FriendRepository;
import repo.UserRepository;
import util.KnowledgeSessionHelper;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.util.*;

public class FriendService {
    private final UserRepository userRepo;
    private final FriendRepository friendRepo;

    public FriendService(UserRepository userRepo, FriendRepository friendRepo) {
        this.userRepo = userRepo;
        this.friendRepo = friendRepo;
    }

    /** Prosta pretraga: ime, prezime, mejl ili mesto (case-insensitive). Isključuje samog sebe. */
    public List<User> searchUsers(String currentUserId, String query, int limit) {
        String q = query == null ? "" : query.trim().toLowerCase();
        int lim = limit <= 0 ? 20 : limit;
        List<User> all = new ArrayList<User>(userRepo.findAll());
        List<User> out = new ArrayList<User>();
        for (User u : all) {
            if (currentUserId != null && currentUserId.equals(u.getId())) continue;
            if (q.isEmpty()
                    || (u.getFirstName() != null && u.getFirstName().toLowerCase().contains(q))
                    || (u.getLastName()  != null && u.getLastName().toLowerCase().contains(q))
                    || (u.getEmail()     != null && u.getEmail().toLowerCase().contains(q))
                    || (u.getCity()      != null && u.getCity().toLowerCase().contains(q))) {
                out.add(u);
                if (out.size() >= lim) break;
            }
        }
        return out;
    }

    /** Validacije kroz pravila; ako sve ok — upiše prijateljstvo simetrično. */
    public void addFriend(String userId, String targetId) {
        ValidationResult vr = new ValidationResult();

        KieContainer kc = KnowledgeSessionHelper.createRuleBase();
        KieSession ks = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            ks.setGlobal("userRepo", userRepo);
            ks.setGlobal("friendRepo", friendRepo);

            ks.insert(new AddFriendRequest(userId, targetId));
            ks.insert(vr);

            ks.getAgenda().getAgendaGroup("friend-add").setFocus();
            ks.fireAllRules();
        } finally {
            ks.dispose();
        }

        if (!vr.isOk()) throw new IllegalArgumentException(String.join("; ", vr.getErrors()));

        // simetrično dodavanje
        friendRepo.addFriends(userId, targetId);
    }
    
    public void blockUser(String userId, String targetId) {
        ValidationResult vr = new ValidationResult();

        KieContainer kc = KnowledgeSessionHelper.createRuleBase();
        KieSession ks = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            ks.setGlobal("userRepo", userRepo);
            ks.setGlobal("friendRepo", friendRepo);

            ks.insert(new BlockUserRequest(userId, targetId));
            ks.insert(vr);

            ks.getAgenda().getAgendaGroup("friend-block").setFocus();
            ks.fireAllRules();
        } finally {
            ks.dispose();
        }

        if (!vr.isOk()) {
            // idempotentno: jedina greška = već blokiran -> tolerisi (no-op)
            if (vr.getErrors().size() == 1 && "Već ste blokirali ovog korisnika.".equals(vr.getErrors().get(0))) {
                return;
            }
            throw new IllegalArgumentException(String.join("; ", vr.getErrors()));
        }

        // ako je sve OK, blokiraj (i skini eventualno prijateljstvo)
        friendRepo.block(userId, targetId);
        repo.ModerationEventsRepository.getInstance().recordBlock(userId, targetId);
    }
    
}
