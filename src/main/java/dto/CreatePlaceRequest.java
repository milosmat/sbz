package dto;

public class CreatePlaceRequest {
    public String adminId;        // ko unosi (mora admin)
    public String name;           // naziv (obavezno)
    public String country;        // država (obavezno)
    public String city;           // grad (obavezno)
    public String description;    // opis (opciono)
    public String hashtagsLine;   // npr: "#film #bioskop" (opciono)

    public CreatePlaceRequest(String adminId, String name, String country, String city,
                              String description, String hashtagsLine) {
        this.adminId = adminId;
        this.name = name;
        this.country = country;
        this.city = city;
        this.description = description;
        this.hashtagsLine = hashtagsLine;
    }
}
