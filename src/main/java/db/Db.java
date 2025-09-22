package db;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Properties;

public final class Db {
    private static String URL;
    private static String USER;
    private static String PASS;

    static {
        try {
            Properties p = new Properties();
            try (InputStream in = Db.class.getClassLoader().getResourceAsStream("db.properties")) {
                if (in == null) throw new IllegalStateException("db.properties not found on classpath");
                p.load(in);
            }
            Class.forName("org.postgresql.Driver"); 
            URL  = p.getProperty("db.url");
            USER = p.getProperty("db.user");
            PASS = p.getProperty("db.pass");
        } catch (Exception e) {
            throw new RuntimeException("DB init failed", e);
        }
    }

    private Db() {}

    public static Connection get() {
        try {
            return DriverManager.getConnection(URL, USER, PASS);
        } catch (Exception e) {
            throw new RuntimeException("DB connect failed", e);
        }
    }
}
