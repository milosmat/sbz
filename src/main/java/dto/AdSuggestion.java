package dto;

import java.util.List;

import model.Place;
import model.Post;

public class AdSuggestion {
    public final Place place;
    public final String why;
    public AdSuggestion(Place place, String why) {
        this.place = place;
        this.why = why;
    }
    
	public Place getPlace() { return place; }
	public String getWhy() { return why; }
}
