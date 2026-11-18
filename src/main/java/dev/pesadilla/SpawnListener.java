package dev.pesadilla;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;

/** Apariciones naturales: vida, equipo, elites, creepers y grupos. */
public final class SpawnListener implements Listener {

    private final PesadillaPlugin plugin;

    public SpawnListener(PesadillaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onSpawn(CreatureSpawnEvent e) {
        if (plugin.levels().factor() <= 0) {
            return;
        }
        if (!plugin.settings().razones.contains(e.getSpawnReason())) {
            return;
        }
        LivingEntity entity = e.getEntity();
        MobProfile profile = plugin.settings().mobs.get(Util.mobId(entity));
        if (profile == null) {
            return;
        }
        plugin.mobs().setupNatural(entity, profile);
    }
}
