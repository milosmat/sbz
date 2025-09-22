package http;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import config.CorsConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class Cors {

    private Cors(){}

    /** Umotaj tvoj handler da automatski rešava CORS (uklj. preflight). */
    public static HttpHandler wrap(HttpHandler next){
        return exchange -> {
            if (handlePreflight(exchange)) return; // OPTIONS preflight rešen ovde
            apply(exchange);                       // dodaj CORS headere pre nego što handler pošalje response
            next.handle(exchange);
        };
    }

    /** Obradi OPTIONS preflight; vrati true ako je odgovor poslat. */
    public static boolean handlePreflight(HttpExchange ex) throws IOException {
        if (!"OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) return false;
        apply(ex);
        ex.sendResponseHeaders(204, -1); // No Content
        ex.close();
        return true;
    }

    /** Dodaj CORS headere na odgovor. Pozovi PRE sendResponseHeaders. */
    public static void apply(HttpExchange ex){
        CorsConfig cfg = CorsConfig.get();
        Headers req = ex.getRequestHeaders();
        Headers res = ex.getResponseHeaders();

        String origin = req.getFirst("Origin");
        String allowOrigin = pickAllowedOrigin(origin, cfg);

        if (allowOrigin != null) {
            res.set("Access-Control-Allow-Origin", allowOrigin);
            res.set("Vary", "Origin");
        }
        res.set("Access-Control-Allow-Methods", cfg.allowedMethods);
        res.set("Access-Control-Allow-Headers", cfg.allowedHeaders);
        res.set("Access-Control-Max-Age", String.valueOf(cfg.maxAge));
        if (cfg.allowCredentials) res.set("Access-Control-Allow-Credentials", "true");

        // default Content-Type (tvoj handler može prebrisati)
        if (!res.containsKey("Content-Type")) {
            res.set("Content-Type", "application/json; charset=UTF-8");
        }
    }

    private static String pickAllowedOrigin(String requestOrigin, CorsConfig cfg){
        if (requestOrigin == null) return null;
        // wildcard nije dozvoljen sa credentials=true, pa echo-uj samo ako je na listi
        return cfg.allowedOrigins.contains(requestOrigin) ? requestOrigin : null;
    }

    // helper za slanje teksta (ako ti zatreba)
    public static void sendText(HttpExchange ex, int status, String body) throws IOException {
        apply(ex);
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, b.length);
        ex.getResponseBody().write(b);
        ex.close();
    }
}
