package primeri;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.Connection;
import java.sql.Statement;

import model.Place;
import model.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import db.Db;
import repo.PlaceRepository;
import repo.UserRepository;
import service.PlaceService;
import dto.CreatePlaceRequest;

public class PlaceServiceTest {

    @Before
    @After
    public void cleanupbefore() throws Exception {
        try (Connection c = Db.get(); Statement st = c.createStatement()) {
            st.executeUpdate("TRUNCATE users CASCADE");
            st.executeUpdate("TRUNCATE places CASCADE");
        }
    }
    
    @Test
    public void admin_kreira_mesto_i_hashtagovi_se_parsiraju() {
        UserRepository ur = new UserRepository();
        PlaceRepository pr = new PlaceRepository();
        PlaceService svc = new PlaceService(pr, ur);

        User admin = new User("Admin","A","admin@ex.com","h","BG");
        ur.save(admin); ur.markAsAdmin(admin.getId());

        Place p = svc.createPlace(new CreatePlaceRequest(
                admin.getId(), "Bioskop Mega", "Srbija", "Beograd", "IMAX sala", "#film #3D #Film"));

        assertThat(p.getName(), is("Bioskop Mega"));
        // lowercase + deduplikacija
        assertThat(p.getHashtags().contains("#film"), is(true));
        assertThat(p.getHashtags().contains("#3d"), is(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void ne_admin_baca_izuzetak() {
        UserRepository ur = new UserRepository();
        PlaceRepository pr = new PlaceRepository();
        PlaceService svc = new PlaceService(pr, ur);

        User user = new User("Pera","Peric","p@e.com","h","BG"); ur.save(user);

        svc.createPlace(new CreatePlaceRequest(
                user.getId(), "Park", "Srbija", "Beograd", "", "#priroda"));
    }
}
