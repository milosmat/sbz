package repo;

import model.Place;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlaceRepository {
    private final Map<String, Place> byId = new ConcurrentHashMap<>();
    // indeks po (name,city) → id, da bismo lako hvatali duplikate
    private final Map<String, String> byNameCityKey = new ConcurrentHashMap<>();

    private String key(String name, String city) {
        String n = name == null ? "" : name.trim().toLowerCase();
        String c = city == null ? "" : city.trim().toLowerCase();
        return n + "|" + c;
    }

    public boolean existsByNameAndCity(String name, String city) {
        return byNameCityKey.containsKey(key(name, city));
    }

    public Place save(Place p) {
        byId.put(p.getId(), p);
        byNameCityKey.put(key(p.getName(), p.getCity()), p.getId());
        return p;
    }

    public boolean existsById(String id) {
        return id != null && byId.containsKey(id);
    }

    public Optional<Place> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    public Collection<Place> findAll() {
        return byId.values();
    }
}
