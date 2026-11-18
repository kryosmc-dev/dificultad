package dev.pesadilla;

import java.util.ArrayList;
import java.util.List;

/** Configuracion de un tipo de mob (una entrada de mobs.yml). */
public final class MobProfile {

    public final String id;
    /** Multiplicador de vida (null = no se toca). */
    public Double vida = null;
    /** Multiplicador de dano base (null = no se toca). */
    public Double dano = null;
    /** Aplica la presion ambiental al dano. */
    public boolean entorno = false;
    public double eliteProb = 0.0;
    public boolean eliteFuerza = false;
    /** cuerpo | arco | ninguno */
    public String equipo = "ninguno";
    public double cargadoProb = 0.0;
    /** Solo creeper: bajo esta Y la explosion crece (null = desactivado). */
    public Double subterraneoY = null;
    public double subterraneoExtra = 1.0;
    public double grupoProb = 0.0;
    public int grupoCantidad = 1;
    public final List<Ability> alAparecer = new ArrayList<>();
    public final List<Ability> alGolpear = new ArrayList<>();
    public final List<Ability> alMorir = new ArrayList<>();

    public MobProfile(String id) {
        this.id = id;
    }
}
