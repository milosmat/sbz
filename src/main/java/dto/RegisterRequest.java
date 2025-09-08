package dto;

public class RegisterRequest {
    public String firstName;
    public String lastName;
    public String email;
    public String password;
    public String city;

    public RegisterRequest(String fn, String ln, String em, String pw, String city) {
        this.firstName = fn;
        this.lastName  = ln;
        this.email     = em;
        this.password  = pw;
        this.city      = city;
    }
}
