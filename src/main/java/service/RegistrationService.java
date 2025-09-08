package service;

import dto.RegisterRequest;
import model.User;
import model.ValidationResult;
import repo.UserRepository;
import util.KnowledgeSessionHelper;   // <— koristi tvoj helper
import util.PasswordHasher;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

public class RegistrationService {
    private final UserRepository repo;

    public RegistrationService(UserRepository repo) {
        this.repo = repo;
    }

    public User register(RegisterRequest req) {
        // 1) pripremi ValidationResult
        ValidationResult vr = new ValidationResult();

        // 2) napravi KieContainer iz helpera
        KieContainer kc = KnowledgeSessionHelper.createRuleBase();

        // 3) uzmi **stateful** sesiju po imenu iz kmodule.xml (npr. "ksession-rules")
        KieSession ksession = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");

        try {
            // 4) postavi globale koje pravila koriste
            ksession.setGlobal("userRepo", repo);
            ksession.setGlobal("EMAIL_RX",
                    java.util.regex.Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"));

            // 5) ubaci činjenice
            ksession.insert(req);
            ksession.insert(vr);

            // 6) fokusiraj našu agendu i aktiviraj pravila
            ksession.getAgenda().getAgendaGroup("registration").setFocus();
            ksession.fireAllRules();
        } finally {
            // 7) obavezno zatvori sesiju
            ksession.dispose();
        }

        // 8) proveri rezultat validacije
        if (!vr.isOk()) {
            throw new IllegalArgumentException(String.join("; ", vr.getErrors()));
        }

        // 9) kreiraj korisnika
        String hash = PasswordHasher.sha256(req.password);
        User u = new User(req.firstName, req.lastName, req.email, hash, req.city);
        return repo.save(u);
    }
}
