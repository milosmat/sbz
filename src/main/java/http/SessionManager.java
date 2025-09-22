package http;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SessionManager {
    private final Map<String, String> sessions = new ConcurrentHashMap<>();
    
    public String createSession(String userId) {
        String token = UUID.randomUUID().toString();
        sessions.put(token, userId);
        return token;
    }

    public Optional<String> getUserId(String token) {
        return Optional.ofNullable(sessions.get(token));
    }

    public void invalidate(String token) {
        sessions.remove(token);
    }
}