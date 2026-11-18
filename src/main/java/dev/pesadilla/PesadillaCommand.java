package dev.pesadilla;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/** /pesadilla (administracion). */
public final class PesadillaCommand implements CommandExecutor, TabCompleter {

    private static final String PERM = "pesadilla.admin";

    private final PesadillaPlugin plugin;

    public PesadillaCommand(PesadillaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Msg msg = plugin.msg();
        if (!sender.hasPermission(PERM)) {
            msg.send(sender, "sin-permiso");
            return true;
        }
        if (args.length == 0) {
            help(sender);
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "nivel" -> level(sender, args);
            case "estado" -> status(sender);
            case "probar" -> test(sender, args);
            case "debug" -> debug(sender);
            case "recargar", "reload" -> reload(sender);
            default -> help(sender);
        }
        return true;
    }

    private void help(CommandSender sender) {
        LevelManager lv = plugin.levels();
        plugin.msg().sendList(sender, "ayuda",
                "niveles", String.join("|", plugin.settings().niveles.keySet()),
                "nivel", lv.name(), "factor", String.valueOf(Util.round2(lv.factor())));
    }

    // ------------------------------------------------------------------
    private void level(CommandSender sender, String[] args) {
        Msg msg = plugin.msg();
        LevelManager lv = plugin.levels();
        if (args.length < 2) {
            msg.send(sender, "nivel-actual", "nivel", lv.name(), "factor", String.valueOf(Util.round2(lv.factor())));
            return;
        }
        String wanted = args[1].toLowerCase(Locale.ROOT);
        if (!lv.exists(wanted)) {
            msg.send(sender, "nivel-invalido", "niveles", String.join(", ", plugin.settings().niveles.keySet()));
            return;
        }
        lv.setLevel(wanted);
        msg.send(sender, "nivel-cambiado", "nivel", lv.name(), "factor", String.valueOf(Util.round2(lv.factor())));
    }

    private void status(CommandSender sender) {
        Msg msg = plugin.msg();
        LevelManager lv = plugin.levels();
        Settings s = plugin.settings();
        msg.send(sender, "nivel-actual", "nivel", lv.name(), "factor", String.valueOf(Util.round2(lv.factor())));
        msg.send(sender, "estado-vida", "valor", String.valueOf(Util.round2(lv.scale(s.baseVida))));
        msg.send(sender, "estado-vida-elite", "valor", String.valueOf(Util.round2(lv.scale(s.baseEliteVida))));
        msg.send(sender, "estado-dano", "valor", String.valueOf(Util.round2(lv.scale(s.baseDano))));
        msg.send(sender, "estado-dano-elite", "valor", String.valueOf(Util.round2(lv.scale(s.baseEliteDano))));
        MobProfile zombie = s.mobs.get("zombie");
        if (zombie != null && zombie.eliteProb > 0) {
            msg.send(sender, "estado-prob", "nombre", "Prob. élite zombi",
                    "valor", String.valueOf(Math.round(lv.effectiveChance(zombie.eliteProb))));
        }
        msg.send(sender, "estado-prob", "nombre", "Prob. horda nocturna",
                "valor", String.valueOf(Math.round(lv.effectiveChance(s.hordaProb))));
        msg.send(sender, "estado-prob", "nombre", "Prob. pesadilla al dormir",
                "valor", String.valueOf(Math.round(lv.effectiveChance(s.camaProb))));
        if (sender instanceof Player p) {
            msg.send(sender, "estado-entorno", "valor", String.valueOf(Util.round2(plugin.env().factor(p))));
        }
    }

    private void test(CommandSender sender, String[] args) {
        Msg msg = plugin.msg();
        if (!(sender instanceof Player p)) {
            msg.send(sender, "solo-jugadores");
            return;
        }
        Vector dir = p.getLocation().getDirection().setY(0);
        if (dir.lengthSquared() < 1.0E-6) {
            dir = new Vector(1, 0, 0);
        }
        Location where = p.getLocation().add(dir.normalize().multiply(3));
        Entity spawned = p.getWorld().spawnEntity(where, EntityType.ZOMBIE);
        if (!(spawned instanceof LivingEntity zombie)) {
            msg.send(sender, "probar-fallo");
            return;
        }
        MobProfile prof = plugin.settings().mobs.get("zombie");
        if (prof != null && prof.vida != null) {
            plugin.mobs().scaleHealth(zombie, plugin.levels().scale(prof.vida));
        }
        boolean elite = args.length > 1 && args[1].equalsIgnoreCase("elite");
        if (elite) {
            plugin.mobs().makeElite(zombie, true);
        }
        String health = String.valueOf(Util.round1(plugin.mobs().maxHealth(zombie)));
        msg.send(sender, elite ? "probar-elite" : "probar-zombi", "vida", health);
    }

    private void debug(CommandSender sender) {
        Msg msg = plugin.msg();
        if (!(sender instanceof Player p)) {
            msg.send(sender, "solo-jugadores");
            return;
        }
        UUID id = p.getUniqueId();
        if (plugin.debugPlayers().remove(id)) {
            msg.send(sender, "debug-off");
        } else {
            plugin.debugPlayers().add(id);
            msg.send(sender, "debug-on");
        }
    }

    private void reload(CommandSender sender) {
        try {
            plugin.reloadAll();
            plugin.msg().send(sender, "recargado", "mobs", String.valueOf(plugin.settings().mobs.size()));
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Error al recargar: " + ex);
            plugin.msg().send(sender, "error-recarga", "error", String.valueOf(ex.getMessage()));
        }
    }

    // ------------------------------------------------------------------
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (!sender.hasPermission(PERM)) {
            return out;
        }
        if (args.length == 1) {
            addMatching(out, List.of("nivel", "estado", "probar", "debug", "recargar"), args[0]);
        } else if (args.length == 2) {
            switch (args[0].toLowerCase(Locale.ROOT)) {
                case "nivel" -> addMatching(out, new ArrayList<>(plugin.settings().niveles.keySet()), args[1]);
                case "probar" -> addMatching(out, List.of("elite"), args[1]);
                default -> {
                }
            }
        }
        return out;
    }

    private void addMatching(List<String> out, List<String> options, String typed) {
        String t = typed.toLowerCase(Locale.ROOT);
        for (String o : options) {
            if (o.toLowerCase(Locale.ROOT).startsWith(t)) {
                out.add(o);
            }
        }
    }
}
