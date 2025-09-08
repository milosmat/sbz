package repo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class FriendRepository {
    // userId -> skup prijatelja
    private final Map<String, Set<String>> friends = new ConcurrentHashMap<>();

    private final Map<String, Set<String>> blocked = new ConcurrentHashMap<>();
    
    public boolean areFriends(String a, String b) {
        if (a == null || b == null) return false;
        Set<String> s = friends.get(a);
        return s != null && s.contains(b);
    }

    public void addFriends(String a, String b) {
        if (a == null || b == null) return;
        friends.computeIfAbsent(a, k -> Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>())).add(b);
        friends.computeIfAbsent(b, k -> Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>())).add(a);
    }

    public void removeFriends(String a, String b) {
        if (a == null || b == null) return;
        Set<String> sa = friends.get(a);
        if (sa != null) sa.remove(b);
        Set<String> sb = friends.get(b);
        if (sb != null) sb.remove(a);
    }
    
    public Set<String> getFriendsOf(String userId) {
        Set<String> s = friends.getOrDefault(userId, Collections.<String>emptySet());
        return new HashSet<>(s);
    }
    
    public boolean isBlocked(String blockerId, String targetId) {
        if (blockerId == null || targetId == null) return false;
        Set<String> s = blocked.get(blockerId);
        return s != null && s.contains(targetId);
    }

    public void block(String blockerId, String targetId) {
        if (blockerId == null || targetId == null) return;
        // blokiranjem se skida eventualno prijateljstvo
        removeFriends(blockerId, targetId);
        blocked.computeIfAbsent(blockerId, k -> Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>()))
               .add(targetId);
    }

    public void unblock(String blockerId, String targetId) {
        Set<String> s = blocked.get(blockerId);
        if (s != null) s.remove(targetId);
    }

    public Set<String> getBlockedBy(String userId) {
        Set<String> s = blocked.getOrDefault(userId, Collections.<String>emptySet());
        return new HashSet<String>(s);
    }
}
