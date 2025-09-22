package util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public final class Hydrator {
    private Hydrator(){}

    public static void set(Object target, String fieldName, Object value) {
        try {
            Field f = findField(target.getClass(), fieldName);
            f.setAccessible(true);
            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(f, f.getModifiers() & ~Modifier.FINAL);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Hydration failed for field: " + fieldName, e);
        }
    }

    private static Field findField(Class<?> cls, String name) throws NoSuchFieldException {
        Class<?> c = cls;
        while (c != null) {
            try { return c.getDeclaredField(name); }
            catch (NoSuchFieldException ignored) { c = c.getSuperclass(); }
        }
        throw new NoSuchFieldException(name);
    }
}
