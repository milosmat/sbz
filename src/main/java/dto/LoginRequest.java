package dto;

public class LoginRequest {
    public String email;
    public String password; // plain unos (heš ćemo porediti u pravilu/servisu)

    public LoginRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }
}
