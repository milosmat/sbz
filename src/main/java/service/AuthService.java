package service;

import dto.LoginRequest;
import model.User;
import model.ValidationResult;
import repo.UserRepository;
import util.KnowledgeSessionHelper;
import util.PasswordHasher;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import java.util.Optional;
import java.util.regex.Pattern;

public class AuthService {
    private final UserRepository repo;

    public AuthService(UserRepository repo) {
        this.repo = repo;
    }

    /** Vraća ulogovanog korisnika ili baca IllegalArgumentException ako validacija padne */
    public User login(LoginRequest req) {
        ValidationResult vr = new ValidationResult();

        KieContainer kc = KnowledgeSessionHelper.createRuleBase();
        KieSession ksession = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session"); // ili "test-session"

        try {
            // globali koje pravila koriste
            ksession.setGlobal("userRepo", repo);
            ksession.setGlobal("EMAIL_RX",
                Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"));

            ksession.insert(req);
            ksession.insert(vr);

            // fokusiraj login agendu i pucaj pravila
            ksession.getAgenda().getAgendaGroup("login").setFocus();
            ksession.fireAllRules();
        } finally {
            ksession.dispose();
        }

        if (!vr.isOk()) {
            throw new IllegalArgumentException(String.join("; ", vr.getErrors()));
        }

        // Ako pravila nisu prijavila grešku -> korisnik postoji i lozinka je tačna
        Optional<User> ou = repo.findByEmail(req.email);
        if (!ou.isPresent()) {
            // teorijski ne bi smelo da se desi jer pravila love "ne postoji korisnik"
            throw new IllegalArgumentException("Korisnik ne postoji");
        }

        User u = ou.get();
        String candidateHash = PasswordHasher.sha256(req.password);
        if (!candidateHash.equals(u.getPasswordHash())) {
            // isto — pravila već love "pogrešna lozinka", ali ostavimo zaštitu
            throw new IllegalArgumentException("Pogrešna lozinka");
        }

        if (repo.isLoginSuspended(u.getId())) {
            long until = repo.loginSuspendedUntil(u.getId());
            throw new IllegalArgumentException("Nalog suspendovan do " + new java.util.Date(until));
        }
        
        return u;
    }
}
