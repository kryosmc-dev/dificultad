package dev.pesadilla;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.EnumMap;
import java.util.Map;

/** Equipo fijo (y vida opcional) para mobs invocados. */
public final class Preset {

    public final Map<EquipmentSlot, ItemStack> items = new EnumMap<>(EquipmentSlot.class);
    /** Vida maxima absoluta (null = no tocar). */
    public Double vida = null;
}
