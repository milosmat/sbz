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
    public static void main(String[] args) {
        UserRepository userRepo = new UserRepository();
        PostRepository postRepo = new PostRepository();
        FriendRepository friendRepo = new FriendRepository();
        PlaceRepository placeRepo = new PlaceRepository();
        FriendService friendService = new FriendService(userRepo, friendRepo);
        PlaceService placeService = new PlaceService(placeRepo, userRepo);
        RegistrationService reg = new RegistrationService(userRepo);
        ModerationService modService = new ModerationService(userRepo, ModerationEventsRepository.getInstance());
        AuthService auth = new AuthService(userRepo);
        PostService postService = new PostService(postRepo, userRepo);
        User currentUser = null;
        
        User admin = reg.register(new RegisterRequest("Admin", "Admin", "admin@sbz.com", "123456", "BG"));
        userRepo.markAsAdmin(admin.getId());
        
        
        System.out.println("=== Društvena mreža (SBZ) ===");
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
			    if ("1".equals(in)) {
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
			    }
			    else if ("2".equals(in)) {
		            try {
		                System.out.print("Mejl: ");    String em = sc.nextLine();
		                System.out.print("Lozinka: "); String pw = sc.nextLine();

		                currentUser = auth.login(new LoginRequest(em, pw));
		                System.out.println("Ulogovan: " + currentUser.getFirstName() + " " + currentUser.getLastName());
		            } catch (IllegalArgumentException ex) {
		                System.out.println("Greška: " + ex.getMessage());
		            }
		        } else if ("3".equals(in)) {
		            if (currentUser == null) {
		                System.out.println("Prijavite se prvo.");
		            } else {
		                List<Post> mine = postService.listMyPosts(currentUser.getId());
		                if (mine.isEmpty()) System.out.println("(Nema objava)");
		                for (Post p : mine) {
		                    System.out.println("- " + p.getCreatedAt() + " | " + p.getText() + " " + p.getHashtags());
		                }
		            }
		        }else if ("4".equals(in)) {
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
		        } else if ("5".equals(in)) {
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
		        } else if ("6".equals(in)) {
		            if (currentUser == null) { System.out.println("Prijavite se prvo."); }
		            else {
		                System.out.print("Pojam: ");
		                String q = sc.nextLine();
		                List<User> res = friendService.searchUsers(currentUser.getId(), q, 20);
		                if (res.isEmpty()) System.out.println("(Nema rezultata)");
		                for (User u : res) {
		                    System.out.println(u.getId()+" | "+u.getFirstName()+" "+u.getLastName()+" | "+u.getEmail()+" | "+u.getCity());
		                }
		            }
		        } else if ("7".equals(in)) {
		            if (currentUser == null) { System.out.println("Prijavite se prvo."); }
		            else {
		                System.out.print("ID korisnika kog dodaješ: ");
		                String tid = sc.nextLine();
		                try {
		                    friendService.addFriend(currentUser.getId(), tid);
		                    System.out.println("Dodati ste u prijatelje.");
		                } catch (IllegalArgumentException ex) {
		                    System.out.println("Greška: " + ex.getMessage());
		                }
		            }
		        } else if ("8".equals(in)) {
		            if (currentUser == null) { System.out.println("Prijavite se prvo."); }
		            else {
		                System.out.print("ID korisnika kog blokiraš: ");
		                String tid = sc.nextLine();
		                try {
		                    friendService.blockUser(currentUser.getId(), tid);
		                    System.out.println("Korisnik je blokiran. (Ako ste bili prijatelji, prijateljstvo je uklonjeno.)");
		                } catch (IllegalArgumentException ex) {
		                    System.out.println("Greška: " + ex.getMessage());
		                }
		            }
		        } else if ("9".equals(in)) {
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
		        } else if ("10".equals(in)) {
		            if (currentUser == null) { System.out.println("Prijavite se prvo."); }
		            else {
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
		        } else if ("11".equals(in)) {
		            if (currentUser == null || !userRepo.isAdmin(currentUser.getId())) {
		                System.out.println("Samo administrator.");
		            } else {
		                java.util.List<repo.ModerationEventsRepository.Flagged> list = modService.detectAndSuspend();
		                if (list.isEmpty()) System.out.println("(Nema sumnjivih korisnika.)");
		                for (repo.ModerationEventsRepository.Flagged f : list) {
		                    System.out.println("- userId=" + f.userId + " | " + f.reason +
		                            (f.until > 0 ? (" | do: " + new java.util.Date(f.until)) : ""));
		                }
		            }
		        } else {
			        System.out.println("Nepoznata opcija.");
			    }
			}
		}
        System.out.println("Doviđenja.");
    }
}
