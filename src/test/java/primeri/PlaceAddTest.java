package primeri;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.Connection;
import java.sql.Statement;

import dto.CreatePlaceRequest;
import model.Place;
import model.User;
import model.ValidationResult;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;

import db.Db;
import repo.PlaceRepository;
import repo.UserRepository;
import util.KnowledgeSessionHelper;

public class PlaceAddTest {

    static KieContainer kc;

    @BeforeClass
    public static void beforeClass() {
        kc = KnowledgeSessionHelper.createRuleBase();
    }

    @Before
    @After
    public void cleanupbefore() throws Exception {
        try (Connection c = Db.get(); Statement st = c.createStatement()) {
            st.executeUpdate("TRUNCATE users CASCADE");
            st.executeUpdate("TRUNCATE places CASCADE");
        }
    }
    
    @Test
    public void adminId_prazan_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            s.setGlobal("userRepo", new UserRepository());
            s.setGlobal("placeRepo", new PlaceRepository());
            ValidationResult vr = new ValidationResult();
            s.insert(new CreatePlaceRequest("", "Bioskop Arena", "Srbija", "Beograd", "opis", "#film"));
            s.insert(vr);
            s.getAgenda().getAgendaGroup("place-add").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1));
        } finally { s.dispose(); }
    }

    @Test
    public void nije_admin_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            UserRepository ur = new UserRepository();
            User u = new User("Pera","Peric","p@e.com","h","BG"); ur.save(u);
            s.setGlobal("userRepo", ur);
            s.setGlobal("placeRepo", new PlaceRepository());
            ValidationResult vr = new ValidationResult();
            s.insert(new CreatePlaceRequest(u.getId(), "Bioskop", "Srbija", "Beograd", "", "#film"));
            s.insert(vr);
            s.getAgenda().getAgendaGroup("place-add").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1));
        } finally { s.dispose(); }
    }

    @Test
    public void naziv_prazan_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            UserRepository ur = new UserRepository();
            User admin = new User("A","D","a@d.com","h","BG"); ur.save(admin); ur.markAsAdmin(admin.getId());
            s.setGlobal("userRepo", ur);
            s.setGlobal("placeRepo", new PlaceRepository());
            ValidationResult vr = new ValidationResult();
            s.insert(new CreatePlaceRequest(admin.getId(), "   ", "Srbija", "Beograd", "", "#film"));
            s.insert(vr);
            s.getAgenda().getAgendaGroup("place-add").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1));
        } finally { s.dispose(); }
    }

    @Test
    public void duplikat_okida1() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            UserRepository ur = new UserRepository();
            User admin = new User("A","D","a@d.com","h","BG"); ur.save(admin); ur.markAsAdmin(admin.getId());

            PlaceRepository pr = new PlaceRepository();
            pr.save(new Place("Bioskop Arena", "Srbija", "Beograd", "", new java.util.HashSet<String>()));

            s.setGlobal("userRepo", ur);
            s.setGlobal("placeRepo", pr);

            ValidationResult vr = new ValidationResult();
            s.insert(new CreatePlaceRequest(admin.getId(), "Bioskop Arena", "Srbija", "Beograd", "opis", "#film"));
            s.insert(vr);
            s.getAgenda().getAgendaGroup("place-add").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(1));
        } finally { s.dispose(); }
    }

    @Test
    public void validno_ne_okida_nista() {
        KieSession s = KnowledgeSessionHelper.getStatefulKnowledgeSession(kc, "test-session");
        try {
            UserRepository ur = new UserRepository();
            User admin = new User("A","D","a@d.com","h","BG"); ur.save(admin); ur.markAsAdmin(admin.getId());
            s.setGlobal("userRepo", ur);
            s.setGlobal("placeRepo", new PlaceRepository());
            ValidationResult vr = new ValidationResult();
            s.insert(new CreatePlaceRequest(admin.getId(), "Planina Tara", "Srbija", "Bajina Bašta", "opis", "#planina #priroda"));
            s.insert(vr);
            s.getAgenda().getAgendaGroup("place-add").setFocus();
            int fired = s.fireAllRules();
            assertThat(fired, is(0));
        } finally { s.dispose(); }
    }
}
