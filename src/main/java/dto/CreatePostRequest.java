package dto;

public class CreatePostRequest {
    public String authorId;     // ulogovani korisnik (obavezno)
    public String text;         // sadržaj objave (obavezno)
    public String hashtagsLine; // npr. "#sbz #java" (opciono)

    public CreatePostRequest(String authorId, String text, String hashtagsLine) {
        this.authorId = authorId;
        this.text = text;
        this.hashtagsLine = hashtagsLine;
    }
}