package repo;

import model.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UserRepository {
    private final Map<String, User> usersById = new ConcurrentHashMap<>();
    private final Map<String, String> idByEmail = new ConcurrentHashMap<>();

    private final java.util.Set<String> adminIds =
    	    java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<String, Boolean>());

    private final java.util.Map<String, Long> postBanUntil  = new java.util.concurrent.ConcurrentHashMap<String, Long>();
    private final java.util.Map<String, Long> loginBanUntil = new java.util.concurrent.ConcurrentHashMap<String, Long>();

    public void suspendPostingHours(String userId, int hours) {
        long until = System.currentTimeMillis() + hours * 3600_000L;
        postBanUntil.merge(userId, until, new java.util.function.BiFunction<Long,Long,Long>() {
            public Long apply(Long oldV, Long newV) { return java.lang.Math.max(oldV, newV); }
        });
    }
    public void suspendLoginHours(String userId,   int hours) {
        long until = System.currentTimeMillis() + hours * 3600_000L;
        loginBanUntil.merge(userId, until, new java.util.function.BiFunction<Long,Long,Long>() {
            public Long apply(Long oldV, Long newV) { return java.lang.Math.max(oldV, newV); }
        });
    }

    public boolean isPostingSuspended(String userId) {
        Long until = postBanUntil.get(userId);
        return until != null && until > System.currentTimeMillis();
    }
    public boolean isLoginSuspended(String userId) {
        Long until = loginBanUntil.get(userId);
        return until != null && until > System.currentTimeMillis();
    }
    public long postingSuspendedUntil(String userId) { Long u = postBanUntil.get(userId); return u == null ? 0L : u; }
    public long loginSuspendedUntil(String userId)   { Long u = loginBanUntil.get(userId); return u == null ? 0L : u; }
    
    public void markAsAdmin(String userId) {
        if (userId != null) adminIds.add(userId);
    }

    public boolean isAdmin(String userId) {
        return userId != null && adminIds.contains(userId);
    }
    
    public Optional<User> findByEmail(String email) {
        if (email == null) return Optional.empty();
        String key = email.toLowerCase().trim();
        String id = idByEmail.get(key);
        return id == null ? Optional.empty() : Optional.ofNullable(usersById.get(id));
    }

    public User save(User u) {
        usersById.put(u.getId(), u);
        idByEmail.put(u.getEmail(), u.getId());
        return u;
    }

    public Collection<User> findAll() {
        return Collections.unmodifiableCollection(usersById.values());
    }
    
    public boolean existsById(String id) {
        return id != null && usersById.containsKey(id);
    }
    
    public Optional<User> findById(String id) {
        return Optional.ofNullable(usersById.get(id));
    }
}
