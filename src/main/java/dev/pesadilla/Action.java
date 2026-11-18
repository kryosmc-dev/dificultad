package dev.pesadilla;

import java.util.Locale;
import java.util.Set;

/** Una accion con formato "tipo:arg:arg" (ver mobs.yml). */
public final class Action {

    /** Tipos cuyo argumento es un texto libre (puede contener ':'). */
    private static final Set<String> TEXT_TYPES = Set.of(
            "mensaje", "titulo", "comando", "anuncio", "comando-todos", "titulo-todos");

    public final String raw;
    public final String type;
    public final String[] args;

    private Action(String raw, String type, String[] args) {
        this.raw = raw;
        this.type = type;
        this.args = args;
    }

    public static Action parse(String raw) {
        String s = raw == null ? "" : raw.trim();
        int i = s.indexOf(':');
        String type = (i < 0 ? s : s.substring(0, i)).trim().toLowerCase(Locale.ROOT);
        String rest = i < 0 ? "" : s.substring(i + 1);
        String[] args;
        if (TEXT_TYPES.contains(type)) {
            args = new String[]{rest};
        } else if (rest.isEmpty()) {
            args = new String[0];
        } else {
            args = rest.split(":");
        }
        return new Action(s, type, args);
    }

    public String arg(int i, String def) {
        if (i >= args.length || args[i].isBlank()) {
            return def;
        }
        return args[i].trim();
    }

    public double num(int i, double def) {
        if (i >= args.length || args[i].isBlank()) {
            return def;
        }
        return Double.parseDouble(args[i].trim());
    }

    public int integer(int i, int def) {
        if (i >= args.length || args[i].isBlank()) {
            return def;
        }
        return (int) Math.round(Double.parseDouble(args[i].trim()));
    }
}
