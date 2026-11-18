package dev.pesadilla;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Motor de habilidades: ejecuta las acciones definidas en mobs.yml. */
public final class Actions {

    /** Contexto de una habilidad. player y mob pueden ser null segun el disparador. */
    public static final class Ctx {
        public final Player player;
        public final LivingEntity mob;
        public final Location loc;
        public final EntityDamageByEntityEvent event;
        public final boolean projectile;

        public Ctx(Player player, LivingEntity mob, Location loc, EntityDamageByEntityEvent event, boolean projectile) {
            this.player = player;
            this.mob = mob;
            this.loc = loc;
            this.event = event;
            this.projectile = projectile;
        }
    }

    private static final Set<String> KNOWN = Set.of(
            "efecto", "efecto-mob", "dano", "lanzar", "empujar", "incendiar", "robo-vida", "mensaje",
            "titulo", "sonido", "invocar", "telarana", "tnt", "rayo", "comando");

    private final PesadillaPlugin plugin;
    private final Set<String> webs = ConcurrentHashMap.newKeySet();
    private final Set<String> warned = ConcurrentHashMap.newKeySet();

    public Actions(PesadillaPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    //  Validacion al cargar la configuracion
    // ------------------------------------------------------------------
    /** Devuelve null si la accion es valida o un texto con el problema. */
    public String validate(Action a) {
        if (!KNOWN.contains(a.type)) {
            return "tipo desconocido '" + a.type + "'";
        }
        try {
            switch (a.type) {
                case "efecto", "efecto-mob" -> {
                    if (a.args.length < 1 || Util.effectType(a.args[0]) == null) {
                        return "efecto desconocido";
                    }
                    a.num(1, 1);
                    a.num(2, 5);
                }
                case "dano", "lanzar", "empujar", "incendiar", "robo-vida" -> {
                    if (a.args.length < 1) {
                        return "falta el valor";
                    }
                    a.num(0, 0);
                }
                case "mensaje", "titulo", "comando" -> {
                    if (a.args.length < 1 || a.args[0].isBlank()) {
                        return "falta el texto";
                    }
                }
                case "sonido" -> {
                    if (a.args.length < 1 || a.args[0].isBlank()) {
                        return "falta el sonido";
                    }
                    a.num(1, 1);
                    a.num(2, 1);
                }
                case "invocar" -> {
                    if (a.args.length < 1 || !Util.isSummonable(Util.entityType(a.args[0]))) {
                        return "mob no invocable";
                    }
                    a.num(1, 1);
                    if (a.args.length > 2 && !plugin.settings().presets.containsKey(a.args[2].trim().toLowerCase(Locale.ROOT))) {
                        return "preset desconocido '" + a.args[2] + "'";
                    }
                }
                case "telarana" -> a.num(0, 6);
                case "tnt" -> {
                    a.num(0, 80);
                    a.num(1, 0);
                }
                default -> {
                }
            }
        } catch (NumberFormatException ex) {
            return "numero invalido";
        }
        return null;
    }

    // ------------------------------------------------------------------
    //  Ejecucion de habilidades
    // ------------------------------------------------------------------
    public void runAbilities(String mobKey, List<Ability> list, Ctx c, String tipo) {
        for (Ability ab : list) {
            if (ab.projectile != null && ab.projectile != c.projectile) {
                continue;
            }
            if (ab.onlyTipo != null && !ab.onlyTipo.equals(tipo)) {
                continue;
            }
            if (ab.exceptTipo != null && ab.exceptTipo.equals(tipo)) {
                continue;
            }
            if (ab.chance < 100.0) {
                boolean ok = ab.scaleChance ? plugin.levels().roll(ab.chance) : Util.chance(ab.chance);
                if (!ok) {
                    continue;
                }
            }
            if (ab.cooldown > 0) {
                UUID who = c.player != null ? c.player.getUniqueId() : (c.mob != null ? c.mob.getUniqueId() : null);
                if (who != null && !plugin.cooldowns().ready(who, mobKey + ":" + ab.id, ab.cooldown)) {
                    continue;
                }
            }
            for (Action a : ab.actions) {
                if (!safeRun(c, a)) {
                    break;
                }
            }
        }
    }

    private boolean safeRun(Ctx c, Action a) {
        try {
            return run(c, a);
        } catch (RuntimeException ex) {
            if (warned.add(a.raw)) {
                plugin.getLogger().warning("Error ejecutando la accion '" + a.raw + "': " + ex);
            }
            return true;
        }
    }

    /** Ejecuta una accion. Devuelve false si hay que cortar el resto de acciones de la habilidad. */
    private boolean run(Ctx c, Action a) {
        Player p = c.player;
        switch (a.type) {
            case "efecto" -> {
                if (p != null) {
                    applyEffect(p, a);
                }
            }
            case "efecto-mob" -> {
                if (c.mob != null) {
                    applyEffect(c.mob, a);
                }
            }
            case "dano" -> {
                if (c.event != null) {
                    c.event.setDamage(c.event.getDamage() * plugin.levels().scale(a.num(0, 1)));
                }
            }
            case "lanzar" -> {
                if (p != null) {
                    final double y = a.num(0, 0.5);
                    later(() -> p.setVelocity(new Vector(0, y, 0)), p);
                }
            }
            case "empujar" -> {
                if (p != null && c.mob != null) {
                    Vector dir = p.getLocation().toVector().subtract(c.mob.getLocation().toVector());
                    dir.setY(0);
                    if (dir.lengthSquared() < 1.0E-6) {
                        dir = new Vector(1, 0, 0);
                    }
                    final Vector push = dir.normalize().multiply(a.num(0, 1)).setY(0.25);
                    later(() -> p.setVelocity(push), p);
                }
            }
            case "incendiar" -> {
                if (p != null) {
                    p.setFireTicks((int) Math.round(a.num(0, 3) * 20.0));
                }
            }
            case "robo-vida" -> {
                if (c.mob != null && c.event != null && !c.mob.isDead()) {
                    AttributeInstance max = c.mob.getAttribute(Attribute.MAX_HEALTH);
                    double cap = max == null ? c.mob.getHealth() : max.getValue();
                    c.mob.setHealth(Math.min(cap, c.mob.getHealth() + c.event.getDamage() * a.num(0, 0.25)));
                }
            }
            case "mensaje" -> {
                if (p != null) {
                    p.sendActionBar(Msg.color(a.args[0]));
                }
            }
            case "titulo" -> {
                if (p != null) {
                    String[] t = a.args[0].split("\\|", 2);
                    p.showTitle(Title.title(Msg.color(t[0]), t.length > 1 ? Msg.color(t[1]) : Component.empty()));
                }
            }
            case "sonido" -> {
                if (p != null) {
                    p.playSound(p.getLocation(), a.arg(0, "entity.zombie.ambient"),
                            (float) a.num(1, 1), (float) a.num(2, 1));
                }
            }
            case "invocar" -> {
                EntityType type = Util.entityType(a.arg(0, ""));
                String presetName = a.arg(2, null);
                Preset preset = presetName == null ? null
                        : plugin.settings().presets.get(presetName.toLowerCase(Locale.ROOT));
                plugin.mobs().summon(c.loc, type, a.integer(1, 1), preset, true);
            }
            case "telarana" -> {
                if (p == null) {
                    return false;
                }
                return placeWeb(p, a.integer(0, 6));
            }
            case "tnt" -> {
                if (c.loc.getWorld() != null) {
                    TNTPrimed tnt = c.loc.getWorld().spawn(c.loc, TNTPrimed.class);
                    tnt.setFuseTicks(a.integer(0, 80));
                    double power = a.num(1, 0);
                    if (power > 0) {
                        tnt.setYield((float) power);
                    }
                }
            }
            case "rayo" -> {
                if (c.loc.getWorld() != null) {
                    if ("real".equalsIgnoreCase(a.arg(0, "efecto"))) {
                        c.loc.getWorld().strikeLightning(c.loc);
                    } else {
                        c.loc.getWorld().strikeLightningEffect(c.loc);
                    }
                }
            }
            case "comando" -> {
                String cmd = a.args[0].replace("%player%", p == null ? "" : p.getName()).trim();
                if (cmd.startsWith("/")) {
                    cmd = cmd.substring(1);
                }
                if (!cmd.isEmpty()) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                }
            }
            default -> {
                return true;
            }
        }
        return true;
    }

    private void applyEffect(LivingEntity target, Action a) {
        PotionEffectType type = Util.effectType(a.arg(0, ""));
        if (type == null) {
            return;
        }
        int level = Math.max(1, a.integer(1, 1));
        int ticks = Math.max(1, (int) Math.round(a.num(2, 5) * 20.0));
        target.addPotionEffect(new PotionEffect(type, ticks, level - 1, false, true, true));
    }

    /** Ejecuta algo un tick despues (para que no lo pise el knockback del golpe). */
    private void later(Runnable r, Player p) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (p.isOnline() && !p.isDead()) {
                r.run();
            }
        });
    }

    // ------------------------------------------------------------------
    //  Telaranas
    // ------------------------------------------------------------------
    private boolean placeWeb(Player p, int seconds) {
        Block b = p.getLocation().getBlock();
        if (!b.getType().isAir()) {
            return false;
        }
        final String key = Util.blockKey(b);
        b.setType(Material.COBWEB);
        webs.add(key);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (webs.remove(key) && b.getType() == Material.COBWEB) {
                b.setType(Material.AIR);
            }
        }, Math.max(1, seconds) * 20L);
        return true;
    }

    /** Quita la marca de una telarana puesta por el plugin. True si lo era. */
    public boolean removeWeb(Block b) {
        return webs.remove(Util.blockKey(b));
    }

    /** Al apagar: quita las telaranas pendientes. */
    public void clearWebs() {
        for (String key : webs) {
            String[] k = key.split(";");
            if (k.length != 4) {
                continue;
            }
            org.bukkit.World w = Bukkit.getWorld(k[0]);
            if (w == null) {
                continue;
            }
            try {
                Block b = w.getBlockAt(Integer.parseInt(k[1]), Integer.parseInt(k[2]), Integer.parseInt(k[3]));
                if (b.getType() == Material.COBWEB) {
                    b.setType(Material.AIR);
                }
            } catch (NumberFormatException ignored) {
                // clave corrupta: se ignora
            }
        }
        webs.clear();
    }
}
