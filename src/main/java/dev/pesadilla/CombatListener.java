package dev.pesadilla;

import org.bukkit.Bukkit;
import org.bukkit.entity.ComplexEntityPart;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.UUID;

/** Cuando un mob golpea a un jugador: dano, presion ambiental, habilidades, medidor y debug. */
public final class CombatListener implements Listener {

    private final PesadillaPlugin plugin;

    public CombatListener(PesadillaPlugin plugin) {
        this.plugin = plugin;
    }

    /** Quien causo realmente el dano (el tirador si es un proyectil). */
    static LivingEntity resolveAttacker(Entity damager) {
        Entity d = damager;
        if (d instanceof ComplexEntityPart part) {
            d = part.getParent();
        }
        if (d instanceof Projectile pr) {
            if (pr.getShooter() instanceof LivingEntity shooter) {
                return shooter;
            }
            return null;
        }
        if (d instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobHitsPlayer(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) {
            return;
        }
        LevelManager lv = plugin.levels();
        if (lv.factor() <= 0) {
            return;
        }
        LivingEntity attacker = resolveAttacker(e.getDamager());
        if (attacker == null || attacker instanceof Player) {
            return;
        }

        Settings s = plugin.settings();
        boolean projectile = e.getDamager() instanceof Projectile;
        String mobId = Util.mobId(attacker);
        String tipo = plugin.mobs().getTipo(attacker);
        MobProfile prof = s.mobs.get(mobId);
        double env = plugin.env().factor(victim);

        // Dano base + presion ambiental + bonus de elite
        if (prof != null) {
            if (prof.dano != null) {
                e.setDamage(e.getDamage() * lv.scale(prof.dano));
            }
            if (prof.entorno) {
                e.setDamage(e.getDamage() * env);
            }
        }
        if ("elite".equals(tipo)) {
            e.setDamage(e.getDamage() * lv.scale(s.baseEliteDano));
        }

        // Habilidades
        if (prof != null && !prof.alGolpear.isEmpty()) {
            Actions.Ctx ctx = new Actions.Ctx(victim, attacker, victim.getLocation(), e, projectile);
            plugin.actions().runAbilities(mobId, prof.alGolpear, ctx, tipo);
        }

        // Medidor y debug
        final UUID id = victim.getUniqueId();
        final String dmg = String.valueOf(Util.round1(e.getDamage()));
        final String envText = String.valueOf(Util.round2(env));
        final String mult = String.valueOf(Util.round2(lv.scale(s.baseDano) * env));
        final String level = lv.name();

        if (plugin.data().hasMedidor(id)) {
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Player p = Bukkit.getPlayer(id);
                if (p != null && p.isOnline()) {
                    plugin.msg().actionBar(p, "medidor-barra", "nivel", level, "entorno", envText,
                            "mult", mult, "golpe", dmg);
                }
            }, s.medidorRetraso);
        }
        if (plugin.debugPlayers().contains(id)) {
            plugin.msg().send(victim, "debug-linea", "mob", mobId, "tipo", tipo, "golpe", dmg,
                    "entorno", envText, "mult", mult);
        }
    }
}
