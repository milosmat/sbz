package dto;
import java.util.Set;
public class FriendIds {
    private final Set<String> ids;
    public FriendIds(Set<String> ids){ this.ids = ids; }
    public Set<String> getIds(){ return ids; }
}
