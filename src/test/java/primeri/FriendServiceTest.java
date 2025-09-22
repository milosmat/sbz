package primeri;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.sql.Connection;
import java.sql.Statement;
import java.util.List;

import model.User;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import db.Db;
import repo.FriendRepository;
import repo.UserRepository;
import service.FriendService;

public class FriendServiceTest {

    @Before
    public void cleanupbefore() throws Exception {
        try (Connection c = Db.get(); Statement st = c.createStatement()) {
            st.executeUpdate("TRUNCATE users CASCADE");
            st.executeUpdate("TRUNCATE friendships CASCADE");
        }
    }
    
    @After
    public void cleanup() throws Exception {
        try (Connection c = Db.get(); Statement st = c.createStatement()) {
            st.executeUpdate("TRUNCATE users CASCADE");
            st.executeUpdate("TRUNCATE friendships CASCADE");
        }
    }
    
    @Test
    public void search_iskljuci_sebe_i_nadji_po_imer_prezime_email_gradu() {
        UserRepository ur = new UserRepository();
        FriendRepository fr = new FriendRepository();
        FriendService svc = new FriendService(ur, fr);

        User ja = new User("Ja","Svoj","ja@ex.com","h","Beograd");
        User a  = new User("Pera","Peric","pera@primer.com","h","Beograd");
        User b  = new User("Mika","Mikic","mika@primer.com","h","Novi Sad");
        ur.save(ja); ur.save(a); ur.save(b);

        List<User> r = svc.searchUsers(ja.getId(), "per", 10); // prezime/ime/email/grad
        // treba da nadje Peru, ali ne i mene
        assertThat(r.isEmpty(), is(false));
        for (User u : r) { assertThat(u.getId().equals(ja.getId()), is(false)); }
    }

    @Test
    public void addFriend_upisuje_simetricno() {
        UserRepository ur = new UserRepository();
        FriendRepository fr = new FriendRepository();
        FriendService svc = new FriendService(ur, fr);

        User u1 = new User("Ana","Anic","ana@ex.com","h","BG"); ur.save(u1);
        User u2 = new User("Mika","Mikic","mika@ex.com","h","BG"); ur.save(u2);

        svc.addFriend(u1.getId(), u2.getId());
        // simetrično su prijatelji
        assertThat(fr.areFriends(u1.getId(), u2.getId()), is(true));
        assertThat(fr.areFriends(u2.getId(), u1.getId()), is(true));
    }
    
    @Test
    public void block_skida_prijateljstvo_i_upisuje_block() {
        UserRepository ur = new UserRepository();
        FriendRepository fr = new FriendRepository();
        FriendService svc = new FriendService(ur, fr);

        User u1 = new User("Ana","Anic","ana@ex.com","h","BG"); ur.save(u1);
        User u2 = new User("Mika","Mikic","mika@ex.com","h","BG"); ur.save(u2);

        fr.addFriends(u1.getId(), u2.getId());
        assertThat(fr.areFriends(u1.getId(), u2.getId()), is(true));

        svc.blockUser(u1.getId(), u2.getId());

        assertThat(fr.areFriends(u1.getId(), u2.getId()), is(false));
        assertThat(fr.isBlocked(u1.getId(), u2.getId()), is(true));
    }

    @Test
    public void block_idempotentno() {
        UserRepository ur = new UserRepository();
        FriendRepository fr = new FriendRepository();
        FriendService svc = new FriendService(ur, fr);

        User a = new User("A","B","a@e.com","h","C"); ur.save(a);
        User b = new User("B","C","b@e.com","h","C"); ur.save(b);

        svc.blockUser(a.getId(), b.getId());
        // drugi put – ne baca izuzetak
        svc.blockUser(a.getId(), b.getId());

        assertThat(fr.isBlocked(a.getId(), b.getId()), is(true));
    }
}
