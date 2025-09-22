package primeri;

import dto.CreatePlaceRequest;
import dto.CreatePostRequest;
import dto.LoginRequest;
import dto.RegisterRequest;
import model.Place;
import model.Post;
import model.User;
import repo.FriendRepository;
import repo.ModerationEventsRepository;
import repo.PlaceRepository;
import repo.PostRepository;
import repo.UserRepository;
import service.AuthService;
import service.FriendService;
import service.ModerationService;
import service.PlaceService;
import service.PostService;
import service.RegistrationService;

import java.util.List;
import java.util.Scanner;

public class App {

    // --- Wiring zajednički za server/CLI ---
    private final UserRepository userRepo = new UserRepository();
    private final PostRepository postRepo = new PostRepository();
    private final FriendRepository friendRepo = new FriendRepository();
    private final PlaceRepository placeRepo = new PlaceRepository();

    private final FriendService friendService = new FriendService(userRepo, friendRepo);
    private final PlaceService placeService = new PlaceService(placeRepo, userRepo);
    private final RegistrationService reg = new RegistrationService(userRepo);
    private final ModerationService modService = new ModerationService(userRepo, ModerationEventsRepository.getInstance());
    private final AuthService auth = new AuthService(userRepo);
    private final PostService postService = new PostService(postRepo, userRepo);

    public static void main(String[] args) throws Exception {
        new App().run(args);
    }

    private void run(String[] args) throws Exception {
        ensureAdminSeed();

        // default: server; "cli" -> CLI meni
        boolean cli = args != null && args.length > 0 && "cli".equalsIgnoreCase(args[0]);
        if (cli) {
            runCli();
        } else {
            startHttpServer();
        }
    }

    // --- SERVER MODE (za Angular frontend) ---
    private void startHttpServer() throws Exception {
        // Pretpostavlja se da imaš klasu http.ApiServer iz prethodnog koraka.
        // Ako nemaš, kaži i ubaciću je ovde.
        System.out.println("[BOOT] Starting HTTP API server on http://localhost:8080 ...");
        http.ApiServer.main(new String[0]); // koristi postojeću implementaciju servera
    }

    // --- CLI MODE (isti meni kao do sada) ---
    private void runCli() {
        User currentUser = null;

        System.out.println("=== Društvena mreža (SBZ) — CLI ===");
        System.out.println("1) Registracija");
        System.out.println("2) Prijava");
        System.out.println("3) Moje objave");
        System.out.println("4) Napiši objavu");
        System.out.println("5) Lajkuj objavu");
        System.out.println("6) Pretraga korisnika");
        System.out.println("7) Dodaj prijatelja");
        System.out.println("8) Blokiraj korisnika");
        System.out.println("9) Prijavi objavu");
        System.out.println("10) Dodaj mesto (ADMIN)");
        System.out.println("11) Detekcija loših korisnika (ADMIN)");
        System.out.println("0) Izlaz");

        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                System.out.print("Izbor: ");
                String in = sc.nextLine();
                if ("0".equals(in)) break;

                switch (in) {
                    case "1": {
                        try {
                            System.out.print("Ime: ");        String fn = sc.nextLine();
                            System.out.print("Prezime: ");    String ln = sc.nextLine();
                            System.out.print("Mejl: ");       String em = sc.nextLine();
                            System.out.print("Lozinka: ");    String pw = sc.nextLine();
                            System.out.print("Mesto: ");      String city = sc.nextLine();

                            RegisterRequest rr = new RegisterRequest(fn, ln, em, pw, city);
                            User u = reg.register(rr);
                            System.out.println("Uspešna registracija! ID: " + u.getId());
                        } catch (IllegalArgumentException ex) {
                            System.out.println("Greška: " + ex.getMessage());
                        }
                        break;
                    }
                    case "2": {
                        try {
                            System.out.print("Mejl: ");    String em = sc.nextLine();
                            System.out.print("Lozinka: "); String pw = sc.nextLine();

                            currentUser = auth.login(new LoginRequest(em, pw));
                            System.out.println("Ulogovan: " + currentUser.getFirstName() + " " + currentUser.getLastName());
                        } catch (IllegalArgumentException ex) {
                            System.out.println("Greška: " + ex.getMessage());
                        }
                        break;
                    }
                    case "3": {
                        if (currentUser == null) {
                            System.out.println("Prijavite se prvo.");
                        } else {
                            List<Post> mine = postService.listMyPosts(currentUser.getId());
                            if (mine.isEmpty()) System.out.println("(Nema objava)");
                            for (Post p : mine) {
                                System.out.println("- " + p.getCreatedAt() + " | " + p.getText() + " " + p.getHashtags());
                            }
                        }
                        break;
                    }
                    case "4": {
                        if (currentUser == null) {
                            System.out.println("Prijavite se prvo.");
                        } else {
                            System.out.print("Tekst: ");
                            String text = sc.nextLine();
                            System.out.print("Heštegovi (npr: #sbz #java): ");
                            String tags = sc.nextLine();

                            try {
                                Post p = postService.createPost(new CreatePostRequest(currentUser.getId(), text, tags));
                                System.out.println("Objava sačuvana! ID: " + p.getId());
                            } catch (IllegalArgumentException ex) {
                                System.out.println("Greška: " + ex.getMessage());
                            }
                        }
                        break;
                    }
                    case "5": {
                        if (currentUser == null) {
                            System.out.println("Prijavite se prvo.");
                        } else {
                            System.out.print("ID objave: ");
                            String pid = sc.nextLine();
                            try {
                                Post p = postService.likePost(currentUser.getId(), pid);
                                System.out.println("Lajkovano! Ukupno lajkova: " + p.getLikes());
                            } catch (IllegalArgumentException ex) {
                                System.out.println("Greška: " + ex.getMessage());
                            }
                        }
                        break;
                    }
                    case "6": {
                        if (currentUser == null) {
                            System.out.println("Prijavite se prvo.");
                        } else {
                            System.out.print("Pojam: ");
                            String q = sc.nextLine();
                            List<User> res = friendService.searchUsers(currentUser.getId(), q, 20);
                            if (res.isEmpty()) System.out.println("(Nema rezultata)");
                            for (User u : res) {
                                System.out.println(u.getId() + " | " + u.getFirstName() + " " + u.getLastName()
                                        + " | " + u.getEmail() + " | " + u.getCity());
                            }
                        }
                        break;
                    }
                    case "7": {
                        if (currentUser == null) {
                            System.out.println("Prijavite se prvo.");
                        } else {
                            System.out.print("ID korisnika kog dodaješ: ");
                            String tid = sc.nextLine();
                            try {
                                friendService.addFriend(currentUser.getId(), tid);
                                System.out.println("Dodati ste u prijatelje.");
                            } catch (IllegalArgumentException ex) {
                                System.out.println("Greška: " + ex.getMessage());
                            }
                        }
                        break;
                    }
                    case "8": {
                        if (currentUser == null) {
                            System.out.println("Prijavite se prvo.");
                        } else {
                            System.out.print("ID korisnika kog blokiraš: ");
                            String tid = sc.nextLine();
                            try {
                                friendService.blockUser(currentUser.getId(), tid);
                                System.out.println("Korisnik je blokiran. (Ako ste bili prijatelji, prijateljstvo je uklonjeno.)");
                            } catch (IllegalArgumentException ex) {
                                System.out.println("Greška: " + ex.getMessage());
                            }
                        }
                        break;
                    }
                    case "9": {
                        if (currentUser == null) {
                            System.out.println("Prijavite se prvo.");
                        } else {
                            System.out.print("ID objave: ");
                            String pid = sc.nextLine();
                            System.out.print("Razlog (opciono): ");
                            String reason = sc.nextLine();
                            try {
                                Post p = postService.reportPost(currentUser.getId(), pid, reason);
                                System.out.println("Prijavljeno. Ukupno prijava: " + p.getReports());
                            } catch (IllegalArgumentException ex) {
                                System.out.println("Greška: " + ex.getMessage());
                            }
                        }
                        break;
                    }
                    case "10": {
                        if (currentUser == null) {
                            System.out.println("Prijavite se prvo.");
                        } else {
                            System.out.print("Naziv: ");       String name = sc.nextLine();
                            System.out.print("Država: ");      String country = sc.nextLine();
                            System.out.print("Grad: ");        String city = sc.nextLine();
                            System.out.print("Opis: ");        String desc = sc.nextLine();
                            System.out.print("Heštegovi: ");   String tags = sc.nextLine();
                            try {
                                Place p = placeService.createPlace(new CreatePlaceRequest(
                                        currentUser.getId(), name, country, city, desc, tags));
                                System.out.println("Mesto dodato! ID: " + p.getId());
                            } catch (IllegalArgumentException ex) {
                                System.out.println("Greška: " + ex.getMessage());
                            }
                        }
                        break;
                    }
                    case "11": {
                        if (currentUser == null || !userRepo.isAdmin(currentUser.getId())) {
                            System.out.println("Samo administrator.");
                        } else {
                            List<repo.ModerationEventsRepository.Flagged> list = modService.detectAndSuspend();
                            if (list.isEmpty()) System.out.println("(Nema sumnjivih korisnika.)");
                            for (repo.ModerationEventsRepository.Flagged f : list) {
                                System.out.println("- userId=" + f.userId + " | " + f.reason +
                                        (f.until > 0 ? (" | do: " + new java.util.Date(f.until)) : ""));
                            }
                        }
                        break;
                    }
                    default:
                        System.out.println("Nepoznata opcija.");
                }
            }
        }
        System.out.println("Doviđenja.");
    }

    // --- Seed admin-a bez dupliranja ---
    private void ensureAdminSeed() {
        final String adminEmail = "admin@sbz.com";
        User admin = userRepo.findByEmail(adminEmail).orElse(null);
        if (admin == null) {
            admin = reg.register(new RegisterRequest("Admin", "Admin", adminEmail, "123456", "BG"));
        }
        userRepo.markAsAdmin(admin.getId());
    }
}
