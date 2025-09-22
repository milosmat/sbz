package repo;

import model.Place;
import db.Db;
import java.sql.*;
import java.util.*;

public class PlaceRepository {

    private String key(String name, String city){
        String n = name == null ? "" : name.trim().toLowerCase();
        String c = city == null ? "" : city.trim().toLowerCase();
        return n + "|" + c;
    }

    public boolean existsByNameAndCity(String name, String city) {
        String sql = "SELECT 1 FROM places WHERE LOWER(name)=LOWER(?) AND LOWER(city)=LOWER(?) LIMIT 1";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name); ps.setString(2, city);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public Place save(Place p) {
        String sql = "INSERT INTO places(id, name, country, city, description, hashtags) " +
                     "VALUES (?,?,?,?,?,?) " +
                     "ON CONFLICT (id) DO UPDATE SET name=EXCLUDED.name, country=EXCLUDED.country, city=EXCLUDED.city, description=EXCLUDED.description, hashtags=EXCLUDED.hashtags";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(p.getId()));
            ps.setString(2, p.getName());
            ps.setString(3, p.getCountry());
            ps.setString(4, p.getCity());
            ps.setString(5, p.getDescription());
            Array arr = c.createArrayOf("text", p.getHashtags().toArray(new String[0]));
            ps.setArray(6, arr);
            ps.executeUpdate();
            return p;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public boolean existsById(String id) {
        if (id == null) return false;
        try (Connection c = Db.get();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM places WHERE id=? LIMIT 1")) {
            ps.setObject(1, java.util.UUID.fromString(id));
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public Optional<Place> findById(String id) {
        if (id == null) return Optional.empty();
        String sql = "SELECT id, name, country, city, description, hashtags FROM places WHERE id=?";
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, java.util.UUID.fromString(id));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(map(rs));
            }
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    public Collection<Place> findAll() {
        String sql = "SELECT id, name, country, city, description, hashtags FROM places";
        List<Place> out = new ArrayList<>();
        try (Connection c = Db.get(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) out.add(map(rs));
            return out;
        } catch (SQLException e) { throw new RuntimeException(e); }
    }

    private Place map(ResultSet rs) throws SQLException {
        String id  = rs.getObject("id", java.util.UUID.class).toString();
        String n   = rs.getString("name");
        String co  = rs.getString("country");
        String ci  = rs.getString("city");
        String d   = rs.getString("description");
        java.sql.Array a = rs.getArray("hashtags");
        Set<String> tags = new HashSet<>();
        if (a != null) tags.addAll(Arrays.asList((String[]) a.getArray()));
        Place p = new Place(n, co, ci, d, tags);
        util.Hydrator.set(p, "id", id); 
        return p;
    }
}
