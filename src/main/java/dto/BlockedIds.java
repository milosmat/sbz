package dto;
import java.util.Set;
public class BlockedIds {
    private final Set<String> ids;
    public BlockedIds(Set<String> ids){ this.ids = ids; }
    public Set<String> getIds(){ return ids; }
}
