package dev.pesadilla;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/** Utilidades generales. */
public final class Util {

    private Util() {
    }

    /** Id del mob en minusculas, p. ej. "zombie", "cave_spider". */
    public static String mobId(Entity e) {
        return e.getType().getKey().getKey();
    }

    /** Entero aleatorio entre min y max, ambos incluidos. */
    public static int rnd(int min, int max) {
        if (max <= min) {
            return min;
        }
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /** Tirada de probabilidad en % (sin escalar por nivel). */
    public static boolean chance(double percent) {
        if (percent <= 0) {
            return false;
        }
        return ThreadLocalRandom.current().nextDouble() * 100.0 < percent;
    }

    public static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    public static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    public static double num(Object o, double def) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        if (o instanceof String s) {
            try {
                return Double.parseDouble(s.trim());
            } catch (NumberFormatException ignored) {
                return def;
            }
        }
        return def;
    }

    public static boolean bool(Object o, boolean def) {
        if (o instanceof Boolean b) {
            return b;
        }
        if (o instanceof String s) {
            return s.equalsIgnoreCase("true");
        }
        return def;
    }

    public static EntityType entityType(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        NamespacedKey key = NamespacedKey.fromString(name.trim().toLowerCase(Locale.ROOT));
        if (key == null) {
            return null;
        }
        return Registry.ENTITY_TYPE.get(key);
    }

    /** True si el tipo es un mob vivo que se puede invocar. */
    public static boolean isSummonable(EntityType type) {
        if (type == null || !type.isSpawnable()) {
            return false;
        }
        Class<? extends Entity> cls = type.getEntityClass();
        return cls != null && LivingEntity.class.isAssignableFrom(cls);
    }

    public static PotionEffectType effectType(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        NamespacedKey key = NamespacedKey.fromString(name.trim().toLowerCase(Locale.ROOT));
        if (key == null) {
            return null;
        }
        return PotionEffectType.getByKey(key);
    }

    public static EquipmentSlot slotFor(String key) {
        if (key == null) {
            return null;
        }
        return switch (key.toLowerCase(Locale.ROOT)) {
            case "casco" -> EquipmentSlot.HEAD;
            case "pechera" -> EquipmentSlot.CHEST;
            case "pantalones" -> EquipmentSlot.LEGS;
            case "botas" -> EquipmentSlot.FEET;
            case "mano" -> EquipmentSlot.HAND;
            case "mano-secundaria" -> EquipmentSlot.OFF_HAND;
            default -> null;
        };
    }

    /** Formato ITEM o ITEM;encanto=nivel,encanto=nivel. Devuelve null si es invalido. */
    public static ItemStack parseItem(String spec) {
        if (spec == null || spec.isBlank()) {
            return null;
        }
        String[] parts = spec.split(";", 2);
        Material m = Material.matchMaterial(parts[0].trim());
        if (m == null || m.isAir() || !m.isItem()) {
            return null;
        }
        ItemStack item = new ItemStack(m);
        if (parts.length > 1) {
            for (String e : parts[1].split(",")) {
                String[] kv = e.split("=");
                NamespacedKey key = NamespacedKey.fromString(kv[0].trim().toLowerCase(Locale.ROOT));
                if (key == null) {
                    continue;
                }
                Enchantment ench = Registry.ENCHANTMENT.get(key);
                if (ench == null) {
                    continue;
                }
                int level = 1;
                if (kv.length > 1) {
                    try {
                        level = Integer.parseInt(kv[1].trim());
                    } catch (NumberFormatException ignored) {
                        level = 1;
                    }
                }
                item.addUnsafeEnchantment(ench, level);
            }
        }
        return item;
    }

    /** Elige una clave segun su peso. Devuelve null si no hay pesos validos. */
    public static String weighted(Map<String, Integer> map) {
        int total = 0;
        for (int w : map.values()) {
            if (w > 0) {
                total += w;
            }
        }
        if (total <= 0) {
            return null;
        }
        int r = rnd(1, total);
        int acc = 0;
        for (Map.Entry<String, Integer> en : map.entrySet()) {
            if (en.getValue() <= 0) {
                continue;
            }
            acc += en.getValue();
            if (r <= acc) {
                return en.getKey();
            }
        }
        return null;
    }

    public static String blockKey(Block b) {
        return b.getWorld().getName() + ";" + b.getX() + ";" + b.getY() + ";" + b.getZ();
    }

    /** Ubicacion en superficie alrededor de un jugador, o null si no encuentra sitio. */
    public static Location randomSurface(Player p, int radius, int minDistance) {
        World w = p.getWorld();
        Location base = p.getLocation();
        for (int i = 0; i < 12; i++) {
            int x = base.getBlockX() + rnd(-radius, radius);
            int z = base.getBlockZ() + rnd(-radius, radius);
            double dx = x - base.getX();
            double dz = z - base.getZ();
            if (Math.sqrt(dx * dx + dz * dz) < minDistance) {
                continue;
            }
            if (!w.isChunkLoaded(x >> 4, z >> 4)) {
                continue;
            }
            Block top = w.getHighestBlockAt(x, z);
            if (top.isLiquid() || !top.getType().isSolid()) {
                continue;
            }
            Block above = top.getRelative(BlockFace.UP);
            if (!above.isPassable() || above.isLiquid() || !above.getRelative(BlockFace.UP).isPassable()) {
                continue;
            }
            return above.getLocation().add(0.5, 0.0, 0.5);
        }
        return null;
    }

    /** Pequeno desplazamiento aleatorio para que varios mobs no nazcan apilados. */
    public static Location spread(Location base) {
        Location l = base.clone().add(ThreadLocalRandom.current().nextDouble(-1.2, 1.2), 0,
                ThreadLocalRandom.current().nextDouble(-1.2, 1.2));
        if (l.getWorld() != null && l.getBlock().isPassable() && l.clone().add(0, 1, 0).getBlock().isPassable()) {
            return l;
        }
        return base.clone();
    }
}
