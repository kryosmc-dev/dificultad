package dev.pesadilla;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** Mensajes configurables (messages.yml) y utilidades de color. */
public final class Msg {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final PesadillaPlugin plugin;
    private YamlConfiguration cfg = new YamlConfiguration();

    public Msg(PesadillaPlugin plugin) {
        this.plugin = plugin;
    }

    public static Component color(String s) {
        return LEGACY.deserialize(s == null ? "" : s);
    }

    public void load() {
        cfg = Util2.loadYaml(plugin, "messages.yml");
    }

    /** Texto crudo con placeholders reemplazados. pairs = clave, valor, clave, valor... */
    public String raw(String key, String... pairs) {
        String s = cfg.getString(key);
        if (s == null) {
            return "&c[mensaje faltante: " + key + "]";
        }
        return fill(s, pairs);
    }

    public List<String> rawList(String key, String... pairs) {
        List<String> out = new ArrayList<>();
        for (String s : cfg.getStringList(key)) {
            out.add(fill(s, pairs));
        }
        return out;
    }

    public static String fill(String s, String... pairs) {
        String r = s;
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            r = r.replace("%" + pairs[i] + "%", pairs[i + 1]);
        }
        return r.replace("%%", "%");
    }

    public void send(CommandSender to, String key, String... pairs) {
        to.sendMessage(color(raw(key, pairs)));
    }

    public void sendList(CommandSender to, String key, String... pairs) {
        for (String line : rawList(key, pairs)) {
            to.sendMessage(color(line));
        }
    }

    public void actionBar(Player p, String key, String... pairs) {
        p.sendActionBar(color(raw(key, pairs)));
    }
}
