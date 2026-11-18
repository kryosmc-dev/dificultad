package dev.pesadilla;

import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

/** Nivel de dificultad activo y sus funciones de escala. */
public final class LevelManager {

    private final PesadillaPlugin plugin;
    private String current;

    public LevelManager(PesadillaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Carga el nivel guardado (o el de por defecto). Llamar despues de cargar Settings y DataStore. */
    public void load() {
        String saved = plugin.data().getLevel();
        Settings s = plugin.settings();
        if (saved != null && s.niveles.containsKey(saved.toLowerCase(Locale.ROOT))) {
            current = saved.toLowerCase(Locale.ROOT);
        } else {
            current = s.nivelDefecto;
        }
    }

    public String name() {
        return current;
    }

    /** Factor del nivel: 0 = apagado, 1 = valores base. */
    public double factor() {
        Double f = plugin.settings().niveles.get(name());
        return f == null ? 1.0 : f;
    }

    /** Escala un multiplicador: factor 1 queda igual; factor 0 queda en 1. */
    public double scale(double base) {
        return 1.0 + (base - 1.0) * factor();
    }

    /** Tirada en % escalada por el nivel (tope 100). */
    public boolean roll(double percent) {
        double p = Math.min(percent * factor(), 100.0);
        if (p <= 0) {
            return false;
        }
        return ThreadLocalRandom.current().nextDouble() * 100.0 < p;
    }

    /** Probabilidad efectiva en % (para mostrarla). */
    public double effectiveChance(double percent) {
        return Math.min(percent * factor(), 100.0);
    }

    public boolean exists(String level) {
        return level != null && plugin.settings().niveles.containsKey(level.toLowerCase(Locale.ROOT));
    }

    public void setLevel(String level) {
        current = level.toLowerCase(Locale.ROOT);
        plugin.data().setLevel(current);
    }
}
