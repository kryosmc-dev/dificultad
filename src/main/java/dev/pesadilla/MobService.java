package dev.pesadilla;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Prepara mobs: vida, equipo, elites, creepers cargados, grupos e invocaciones. */
public final class MobService {

    private final PesadillaPlugin plugin;

    public MobService(PesadillaPlugin plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    //  Marca interna (persistent data) - invisible para los jugadores
    // ------------------------------------------------------------------
    public void setTipo(Entity e, String tipo) {
        e.getPersistentDataContainer().set(plugin.tipoKey(), PersistentDataType.STRING, tipo);
    }

    /** "elite", "cargado", "invocado" o "ninguno". */
    public String getTipo(Entity e) {
        String v = e.getPersistentDataContainer().get(plugin.tipoKey(), PersistentDataType.STRING);
        return v == null ? "ninguno" : v;
    }

    // ------------------------------------------------------------------
    //  Vida
    // ------------------------------------------------------------------
    public void scaleHealth(LivingEntity e, double mult) {
        AttributeInstance a = e.getAttribute(Attribute.MAX_HEALTH);
        if (a == null) {
            return;
        }
        a.setBaseValue(a.getBaseValue() * mult);
        e.setHealth(a.getValue());
    }

    public void setMaxHealth(LivingEntity e, double value) {
        AttributeInstance a = e.getAttribute(Attribute.MAX_HEALTH);
        if (a == null) {
            return;
        }
        a.setBaseValue(Math.max(1.0, value));
        e.setHealth(a.getValue());
    }

    public double maxHealth(LivingEntity e) {
        AttributeInstance a = e.getAttribute(Attribute.MAX_HEALTH);
        return a == null ? e.getHealth() : a.getValue();
    }

    // ------------------------------------------------------------------
    //  Aparicion natural
    // ------------------------------------------------------------------
    public void setupNatural(LivingEntity e, MobProfile p) {
        LevelManager lv = plugin.levels();

        if (p.vida != null) {
            scaleHealth(e, lv.scale(p.vida));
        }
        if (!"ninguno".equals(p.equipo)) {
            randomGear(e, p.equipo);
        }
        if (p.eliteProb > 0 && lv.roll(p.eliteProb)) {
            makeElite(e, p.eliteFuerza);
        }
        if (e instanceof Creeper creeper) {
            if (p.subterraneoY != null && e.getLocation().getY() < p.subterraneoY) {
                int radius = (int) Math.round(3.0 + p.subterraneoExtra * lv.factor());
                creeper.setExplosionRadius(Math.max(0, radius));
            }
            if (p.cargadoProb > 0 && lv.roll(p.cargadoProb)) {
                creeper.setPowered(true);
                setTipo(e, "cargado");
            }
        }
        if (p.grupoProb > 0 && lv.roll(p.grupoProb)) {
            final Location where = e.getLocation().clone();
            final EntityType type = e.getType();
            final int amount = p.grupoCantidad;
            // se invoca un tick despues para no crear entidades dentro del evento de spawn
            Bukkit.getScheduler().runTask(plugin, () -> summon(where, type, amount, null, false));
        }
        if (!p.alAparecer.isEmpty()) {
            Actions.Ctx ctx = new Actions.Ctx(null, e, e.getLocation(), null, false);
            plugin.actions().runAbilities(p.id, p.alAparecer, ctx, getTipo(e));
        }
    }

    // ------------------------------------------------------------------
    //  Elites
    // ------------------------------------------------------------------
    public void makeElite(LivingEntity e, boolean fuerza) {
        Settings s = plugin.settings();
        scaleHealth(e, plugin.levels().scale(s.baseEliteVida));
        setTipo(e, "elite");
        for (String eff : s.eliteEfectos) {
            applyInfinite(e, eff, s.eliteOcultarParticulas);
        }
        if (fuerza && s.eliteFuerza != null && !s.eliteFuerza.isBlank()) {
            applyInfinite(e, s.eliteFuerza, s.eliteOcultarParticulas);
        }
        if (s.eliteBrillo) {
            e.setGlowing(true);
        }
        if (s.eliteNombre != null && !s.eliteNombre.isBlank()) {
            e.customName(Msg.color(s.eliteNombre));
            e.setCustomNameVisible(true);
        }
    }

    private void applyInfinite(LivingEntity e, String spec, boolean hide) {
        String[] parts = spec.split(":");
        PotionEffectType type = Util.effectType(parts[0]);
        if (type == null) {
            plugin.getLogger().warning("Efecto de elite desconocido: " + spec);
            return;
        }
        int level = 1;
        if (parts.length > 1) {
            try {
                level = Math.max(1, Integer.parseInt(parts[1].trim()));
            } catch (NumberFormatException ignored) {
                level = 1;
            }
        }
        e.addPotionEffect(new PotionEffect(type, PotionEffect.INFINITE_DURATION, level - 1, false, !hide, !hide));
    }

    // ------------------------------------------------------------------
    //  Equipo
    // ------------------------------------------------------------------
    public void randomGear(LivingEntity e, String tipo) {
        EntityEquipment eq = e.getEquipment();
        if (eq == null || "ninguno".equals(tipo)) {
            return;
        }
        Settings s = plugin.settings();
        applyTier(eq, roll(s.armaduras));
        applyTier(eq, roll("arco".equals(tipo) ? s.arco : s.cuerpo));
        applyDropChance(e);
    }

    private GearTier roll(List<GearTier> tiers) {
        int r = Util.rnd(1, 100);
        int acc = 0;
        for (GearTier t : tiers) {
            acc += t.prob;
            if (r <= acc) {
                return t;
            }
        }
        return null;
    }

    private void applyTier(EntityEquipment eq, GearTier tier) {
        if (tier == null) {
            return;
        }
        for (Map.Entry<EquipmentSlot, ItemStack> en : tier.items.entrySet()) {
            eq.setItem(en.getKey(), en.getValue().clone());
        }
    }

    public void applyPreset(LivingEntity e, Preset preset) {
        EntityEquipment eq = e.getEquipment();
        if (eq != null) {
            for (Map.Entry<EquipmentSlot, ItemStack> en : preset.items.entrySet()) {
                eq.setItem(en.getKey(), en.getValue().clone());
            }
            applyDropChance(e);
        }
        if (preset.vida != null) {
            setMaxHealth(e, preset.vida);
        }
    }

    private void applyDropChance(LivingEntity e) {
        double chance = plugin.settings().dropChance;
        EntityEquipment eq = e.getEquipment();
        if (chance < 0 || eq == null || !(e instanceof Mob)) {
            return;
        }
        float f = (float) Math.min(1.0, chance);
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot == EquipmentSlot.BODY) {
                continue;
            }
            eq.setDropChance(slot, f);
        }
    }

    // ------------------------------------------------------------------
    //  Invocaciones
    // ------------------------------------------------------------------
    /**
     * Invoca mobs marcados como "invocado".
     *
     * @param spread     true = reparte un poco los mobs alrededor de base
     */
    public List<LivingEntity> summon(Location base, EntityType type, int count, Preset preset, boolean spread) {
        List<LivingEntity> out = new ArrayList<>();
        if (base.getWorld() == null || !Util.isSummonable(type)) {
            return out;
        }
        for (int i = 0; i < count; i++) {
            Location l = spread ? Util.spread(base) : base.clone();
            Entity en = base.getWorld().spawnEntity(l, type);
            if (!(en instanceof LivingEntity le)) {
                en.remove();
                continue;
            }
            setTipo(le, "invocado");
            if (plugin.settings().invocadosEscalanVida) {
                MobProfile mp = plugin.settings().mobs.get(Util.mobId(le));
                if (mp != null && mp.vida != null) {
                    scaleHealth(le, plugin.levels().scale(mp.vida));
                }
            }
            if (preset != null) {
                applyPreset(le, preset);
            }
            out.add(le);
        }
        return out;
    }

    /** Invoca un mob para hordas/pesadillas, con equipo aleatorio segun su perfil. */
    public LivingEntity summonWithGear(Location where, EntityType type) {
        List<LivingEntity> list = summon(where, type, 1, null, false);
        if (list.isEmpty()) {
            return null;
        }
        LivingEntity le = list.get(0);
        MobProfile mp = plugin.settings().mobs.get(Util.mobId(le));
        if (mp != null && !"ninguno".equals(mp.equipo)) {
            randomGear(le, mp.equipo);
        }
        return le;
    }

    /** Clave NamespacedKey de la marca interna. */
    public NamespacedKey key() {
        return plugin.tipoKey();
    }
}
