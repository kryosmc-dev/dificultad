package dev.pesadilla;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/** Un escalon de equipo aleatorio con su probabilidad. */
public final class GearTier {

    public final int prob;
    public final Map<EquipmentSlot, ItemStack> items = new EnumMap<>(EquipmentSlot.class);

    public GearTier(int prob) {
        this.prob = prob;
    }
}
