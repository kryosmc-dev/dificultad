package dev.pesadilla;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Cooldowns por entidad y clave. */
public final class Cooldowns {

    private final Map<UUID, Map<String, Long>> map = new ConcurrentHashMap<>();

    /** Devuelve true (y arranca el cooldown) si ya paso el tiempo. */
    public boolean ready(UUID id, String key, double seconds) {
        if (seconds <= 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        Map<String, Long> m = map.computeIfAbsent(id, k -> new ConcurrentHashMap<>());
        Long last = m.get(key);
        if (last != null && now - last < (long) (seconds * 1000.0)) {
            return false;
        }
        m.put(key, now);
        return true;
    }

    public void clear(UUID id) {
        map.remove(id);
    }

    public void clearAll() {
        map.clear();
    }
}
