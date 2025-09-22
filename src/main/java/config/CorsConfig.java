package config;

import java.io.InputStream;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Properties;
import java.util.Set;

public final class CorsConfig {
    public final Set<String> allowedOrigins;
    public final String allowedMethods;
    public final String allowedHeaders;
    public final boolean allowCredentials;
    public final int maxAge;

    private static CorsConfig INSTANCE;

    private CorsConfig(Set<String> origins, String methods, String headers, boolean creds, int maxAge){
        this.allowedOrigins = origins;
        this.allowedMethods = methods;
        this.allowedHeaders = headers;
        this.allowCredentials = creds;
        this.maxAge = maxAge;
    }

    public static CorsConfig get(){
        if (INSTANCE != null) return INSTANCE;
        try {
            Properties p = new Properties();
            try (InputStream in = CorsConfig.class.getClassLoader().getResourceAsStream("cors.properties")) {
                if (in != null) p.load(in);
            }
            Set<String> origins = new LinkedHashSet<>();
            String raw = p.getProperty("cors.allowedOrigins", "http://localhost:4200");
            for (String o : raw.split(",")) { String t = o.trim(); if (!t.isEmpty()) origins.add(t); }
            String methods = p.getProperty("cors.allowedMethods", "GET,POST,PUT,PATCH,DELETE,OPTIONS");
            String headers = p.getProperty("cors.allowedHeaders", "Content-Type,Authorization");
            boolean creds = Boolean.parseBoolean(p.getProperty("cors.allowCredentials", "true"));
            int maxAge = Integer.parseInt(p.getProperty("cors.maxAge", "3600"));
            INSTANCE = new CorsConfig(origins, methods, headers, creds, maxAge);
            return INSTANCE;
        } catch (Exception e) {
            // fallback default
            return (INSTANCE = new CorsConfig(
                new LinkedHashSet<>(Arrays.asList("http://localhost:4200")),
                "GET,POST,PUT,PATCH,DELETE,OPTIONS",
                "Content-Type,Authorization", true, 3600
            ));
        }
    }
}
