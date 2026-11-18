package dev.pesadilla;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/** Multiplicador de dano segun distancia al spawn, profundidad, dimension y equipo. */
public final class EnvironmentFactor {

    private final PesadillaPlugin plugin;

    public EnvironmentFactor(PesadillaPlugin plugin) {
        this.plugin = plugin;
    }

    public double factor(Player p) {
        Settings s = plugin.settings();
        World w = p.getWorld();
        Location loc = p.getLocation();
        double f = 1.0;

        Double dim = s.dimensiones.get(w.getEnvironment().name());
        if (dim != null) {
            f += dim;
        } else {
            double d = loc.distance(w.getSpawnLocation());
            f += Math.min(d / s.distBloques, s.distMaxTramos) * s.distDano;
            double prof = Math.max(s.profYBase - loc.getY(), 0.0);
            f += Math.min((prof / s.profPaso) * s.profDano, s.profTope);
        }

        for (ItemStack piece : p.getInventory().getArmorContents()) {
            if (piece == null) {
                continue;
            }
            Double bonus = s.armaduraBonus.get(piece.getType());
            if (bonus != null) {
                f += bonus;
            }
        }

        f = Math.min(f, s.entornoMax);
        return 1.0 + (f - 1.0) * plugin.levels().factor();
    }
}
