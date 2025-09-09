package service;

import model.User;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import repo.ModerationEventsRepository;
import repo.UserRepository;
import util.KnowledgeSessionHelper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ModerationService {
    private final UserRepository userRepo;
    private final ModerationEventsRepository modRepo;

    public ModerationService(UserRepository userRepo, ModerationEventsRepository modRepo) {
        this.userRepo = userRepo;
        this.modRepo = modRepo;
    }

    /** Pokreće pravila nad svim korisnicima i vraća listu označenih/suspendovanih. */
    public List<ModerationEventsRepository.Flagged> detectAndSuspend() {
        KieContainer kc = KnowledgeSessionHelper.createRuleBase();
        KieSession ks = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            ks.setGlobal("userRepo", userRepo);
            ks.setGlobal("modRepo", modRepo);

            Collection<User> all = userRepo.findAll();
            for (User u : all) ks.insert(u);

            for (ModerationEventsRepository.ReportEvent r : modRepo.getReports()) {
                ks.insert(r);
            }

            for (ModerationEventsRepository.BlockEvent b : modRepo.getBlocks()) {
                ks.insert(b);
            }
            
            ks.getAgenda().getAgendaGroup("user-detect").setFocus();
            ks.fireAllRules();
        } finally {
            ks.dispose();
        }
        return new ArrayList<>(modRepo.getFlagsAndClear());
    }
}
