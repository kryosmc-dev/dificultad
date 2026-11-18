package dev.pesadilla;

import java.util.ArrayList;
import java.util.List;

/** Habilidad de un mob: condiciones + lista de acciones. */
public final class Ability {

    public final String id;
    public double chance = 100.0;
    public boolean scaleChance = true;
    public double cooldown = 0.0;
    /** null = cualquiera, true = solo proyectil, false = solo cuerpo a cuerpo. */
    public Boolean projectile = null;
    public String onlyTipo = null;
    public String exceptTipo = null;
    public final List<Action> actions = new ArrayList<>();

    public Ability(String id) {
        this.id = id;
    }
}
