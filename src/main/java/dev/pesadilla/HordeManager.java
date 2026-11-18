package dev.pesadilla;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/** Hordas nocturnas y pesadilla al dormir. */
public final class HordeManager {

    private final PesadillaPlugin plugin;
    private BukkitTask task;

    public HordeManager(PesadillaPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        Settings s = plugin.settings();
        if (!s.hordaActiva) {
            return;
        }
        long period = Math.max(20L, Math.round(s.hordaMinutos * 60.0 * 20.0));
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, period, period);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    private void tick() {
        Settings s = plugin.settings();
        if (!s.hordaActiva || plugin.levels().factor() <= 0) {
            return;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            World w = p.getWorld();
            if (w.getEnvironment() == World.Environment.NETHER || w.getEnvironment() == World.Environment.THE_END) {
                continue;
            }
            if (s.hordaIgnorar.contains(p.getGameMode()) || p.isDead()) {
                continue;
            }
            if (!isNight(w.getTime(), s)) {
                continue;
            }
            Location loc = p.getLocation();
            if (loc.getY() <= s.hordaYMin) {
                continue;
            }
            if (s.hordaSoloCielo && w.getHighestBlockYAt(loc) > loc.getBlockY()) {
                continue;
            }
            if (!plugin.levels().roll(s.hordaProb)) {
                continue;
            }
            spawnHorde(p);
        }
    }

    private boolean isNight(long time, Settings s) {
        if (s.hordaInicio <= s.hordaFin) {
            return time >= s.hordaInicio && time <= s.hordaFin;
        }
        return time >= s.hordaInicio || time <= s.hordaFin;
    }

    public void spawnHorde(Player p) {
        Settings s = plugin.settings();
        plugin.msg().actionBar(p, "horda-aviso");
        p.playSound(p.getLocation(), "ambient.cave", 1.0f, 1.0f);
        int n = Util.rnd(s.hordaMin, s.hordaMax);
        for (int i = 0; i < n; i++) {
            spawnOne(p, s.hordaMobs, s.hordaRadio, s.hordaDistMin);
        }
    }

    /** Pesadilla al entrar en una cama. */
    public void nightmare(Player p) {
        Settings s = plugin.settings();
        if (!s.camaActiva || plugin.levels().factor() <= 0) {
            return;
        }
        if (!plugin.levels().roll(s.camaProb)) {
            return;
        }
        plugin.msg().actionBar(p, "cama-aviso");
        p.playSound(p.getLocation(), "ambient.cave", 1.0f, 1.0f);
        for (int i = 0; i < s.camaCantidad; i++) {
            spawnOne(p, s.camaMobs, s.camaRadio, s.camaDistMin);
        }
    }

    private void spawnOne(Player p, java.util.Map<String, Integer> weights, int radius, int minDist) {
        String id = Util.weighted(weights);
        if (id == null) {
            return;
        }
        EntityType type = Util.entityType(id);
        Location l = Util.randomSurface(p, radius, minDist);
        if (type == null || l == null) {
            return;
        }
        plugin.mobs().summonWithGear(l, type);
    }
}
