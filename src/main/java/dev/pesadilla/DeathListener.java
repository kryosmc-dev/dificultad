package dev.pesadilla;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

/** Muerte de mobs a manos de jugadores: elites y habilidades al morir. */
public final class DeathListener implements Listener {

    private final PesadillaPlugin plugin;

    public DeathListener(PesadillaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onDeath(EntityDeathEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim instanceof Player) {
            return;
        }
        Player killer = victim.getKiller();
        if (killer == null) {
            return;
        }
        String tipo = plugin.mobs().getTipo(victim);

        LevelManager lv = plugin.levels();
        if (lv.factor() <= 0) {
            return;
        }
        Settings s = plugin.settings();
        Location where = victim.getLocation();

        // Elites: XP extra y a veces secuaces
        if ("elite".equals(tipo)) {
            e.setDroppedExp(e.getDroppedExp() + s.eliteXp);
            if (s.venganzaCantidad > 0 && lv.roll(s.venganzaProb)) {
                plugin.mobs().summon(where, victim.getType(), s.venganzaCantidad, null, true);
            }
            return;
        }

        // Lo invocado por el plugin no vuelve a invocar
        if ("invocado".equals(tipo)) {
            return;
        }

        String mobId = Util.mobId(victim);
        MobProfile prof = s.mobs.get(mobId);
        if (prof == null || prof.alMorir.isEmpty()) {
            return;
        }
        Actions.Ctx ctx = new Actions.Ctx(killer, victim, where, null, false);
        plugin.actions().runAbilities(mobId, prof.alMorir, ctx, tipo);
    }
}
