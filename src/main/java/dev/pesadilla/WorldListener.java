package dev.pesadilla;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Ghast;
import org.bukkit.entity.LargeFireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;

/** Camas, telaranas, bolas de fuego de ghasts y limpieza al salir. */
public final class WorldListener implements Listener {

    private final PesadillaPlugin plugin;

    public WorldListener(PesadillaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBed(PlayerBedEnterEvent e) {
        if (e.getBedEnterResult() != PlayerBedEnterEvent.BedEnterResult.OK) {
            return;
        }
        plugin.horde().nightmare(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onBreakWeb(BlockBreakEvent e) {
        Block b = e.getBlock();
        if (b.getType() != Material.COBWEB || !plugin.actions().removeWeb(b)) {
            return;
        }
        Ability ab = plugin.settings().telaranaAlRomper;
        if (ab == null || plugin.levels().factor() <= 0) {
            return;
        }
        Actions.Ctx ctx = new Actions.Ctx(e.getPlayer(), null, b.getLocation().add(0.5, 0.0, 0.5), null, false);
        plugin.actions().runAbilities("telarana", List.of(ab), ctx, "ninguno");
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onExplosionPrime(ExplosionPrimeEvent e) {
        Settings s = plugin.settings();
        if (!s.ghastActivo || plugin.levels().factor() <= 0) {
            return;
        }
        if (!(e.getEntity() instanceof Fireball fireball)) {
            return;
        }
        if (s.ghastSoloGhast && !(fireball instanceof LargeFireball && fireball.getShooter() instanceof Ghast)) {
            return;
        }
        e.setRadius((float) plugin.levels().scale(s.ghastRadio));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        plugin.cooldowns().clear(p.getUniqueId());
        plugin.debugPlayers().remove(p.getUniqueId());
    }
}
