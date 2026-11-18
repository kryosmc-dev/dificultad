package dev.pesadilla;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.GameMode;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/** Lee config.yml y mobs.yml y los deja listos en memoria. */
public final class Settings {

    private final PesadillaPlugin plugin;
    private final Logger log;

    // ---- niveles y bases ----
    public String nivelDefecto = "veterano";
    public final Map<String, Double> niveles = new LinkedHashMap<>();
    public double baseVida = 1.5;
    public double baseDano = 1.15;
    public double baseEliteVida = 2.0;
    public double baseEliteDano = 1.3;
    public final Set<CreatureSpawnEvent.SpawnReason> razones = EnumSet.noneOf(CreatureSpawnEvent.SpawnReason.class);
    public boolean invocadosEscalanVida = false;

    // ---- entorno ----
    public double distBloques = 500;
    public int distMaxTramos = 8;
    public double distDano = 0.04;
    public double profYBase = 64;
    public double profPaso = 10;
    public double profDano = 0.03;
    public double profTope = 0.4;
    public double entornoMax = 1.9;
    public final Map<String, Double> dimensiones = new HashMap<>();
    public final Map<Material, Double> armaduraBonus = new EnumMap<>(Material.class);

    // ---- elite ----
    public int eliteXp = 12;
    public final List<String> eliteEfectos = new ArrayList<>();
    public String eliteFuerza = "strength:1";
    public boolean eliteOcultarParticulas = false;
    public boolean eliteBrillo = false;
    public String eliteNombre = "";
    public double venganzaProb = 35;
    public int venganzaCantidad = 2;

    // ---- equipo ----
    public double dropChance = 0.085;
    public final List<GearTier> armaduras = new ArrayList<>();
    public final List<GearTier> cuerpo = new ArrayList<>();
    public final List<GearTier> arco = new ArrayList<>();

    // ---- horda ----
    public boolean hordaActiva = true;
    public double hordaMinutos = 6;
    public double hordaProb = 35;
    public long hordaInicio = 12000;
    public long hordaFin = 23000;
    public int hordaYMin = 50;
    public int hordaRadio = 20;
    public int hordaDistMin = 8;
    public int hordaMin = 3;
    public int hordaMax = 5;
    public boolean hordaSoloCielo = false;
    public final Set<GameMode> hordaIgnorar = EnumSet.noneOf(GameMode.class);
    public final Map<String, Integer> hordaMobs = new LinkedHashMap<>();

    // ---- cama ----
    public boolean camaActiva = true;
    public double camaProb = 25;
    public int camaRadio = 6;
    public int camaDistMin = 2;
    public int camaCantidad = 2;
    public final Map<String, Integer> camaMobs = new LinkedHashMap<>();

    // ---- ghast ----
    public boolean ghastActivo = true;
    public double ghastRadio = 2.0;
    public boolean ghastSoloGhast = true;

    // ---- telarana ----
    public Ability telaranaAlRomper = null;

    // ---- otros ----
    public int medidorRetraso = 40;
    public int autoguardadoMin = 5;

    // ---- mobs.yml ----
    public final Map<String, MobProfile> mobs = new LinkedHashMap<>();
    public final Map<String, Preset> presets = new HashMap<>();

    public Settings(PesadillaPlugin plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public void load() {
        plugin.reloadConfig();
        FileConfiguration c = plugin.getConfig();

        nivelDefecto = c.getString("nivel-por-defecto", "veterano").toLowerCase(Locale.ROOT);
        niveles.clear();
        ConfigurationSection lv = c.getConfigurationSection("niveles");
        if (lv != null) {
            for (String k : lv.getKeys(false)) {
                niveles.put(k.toLowerCase(Locale.ROOT), Math.max(0.0, lv.getDouble(k)));
            }
        }
        if (niveles.isEmpty()) {
            niveles.put("apagado", 0.0);
            niveles.put("veterano", 1.15);
        }
        if (!niveles.containsKey(nivelDefecto)) {
            log.warning("nivel-por-defecto '" + nivelDefecto + "' no existe en 'niveles'; usando el primero.");
            nivelDefecto = niveles.keySet().iterator().next();
        }

        baseVida = c.getDouble("base.vida", 1.5);
        baseDano = c.getDouble("base.dano", 1.15);
        baseEliteVida = c.getDouble("base.elite-vida", 2.0);
        baseEliteDano = c.getDouble("base.elite-dano", 1.3);

        razones.clear();
        for (String r : c.getStringList("spawn.razones")) {
            try {
                razones.add(CreatureSpawnEvent.SpawnReason.valueOf(r.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                log.warning("spawn.razones: motivo desconocido '" + r + "'");
            }
        }
        invocadosEscalanVida = c.getBoolean("spawn.invocados-escalan-vida", false);

        distBloques = Math.max(1.0, c.getDouble("entorno.distancia.por-bloques", 500));
        distMaxTramos = Math.max(0, c.getInt("entorno.distancia.max-tramos", 8));
        distDano = c.getDouble("entorno.distancia.dano", 0.04);
        profYBase = c.getDouble("entorno.profundidad.y-base", 64);
        profPaso = Math.max(1.0, c.getDouble("entorno.profundidad.por-bloques", 10));
        profDano = c.getDouble("entorno.profundidad.dano", 0.03);
        profTope = c.getDouble("entorno.profundidad.tope", 0.4);
        entornoMax = c.getDouble("entorno.maximo", 1.9);
        dimensiones.clear();
        ConfigurationSection dim = c.getConfigurationSection("entorno.dimensiones");
        if (dim != null) {
            for (String k : dim.getKeys(false)) {
                dimensiones.put(k.toUpperCase(Locale.ROOT), dim.getDouble(k));
            }
        }
        armaduraBonus.clear();
        ConfigurationSection arm = c.getConfigurationSection("entorno.armadura");
        if (arm != null) {
            for (String k : arm.getKeys(false)) {
                Material m = Material.matchMaterial(k);
                if (m == null) {
                    log.warning("entorno.armadura: material desconocido '" + k + "'");
                    continue;
                }
                armaduraBonus.put(m, arm.getDouble(k));
            }
        }

        eliteXp = c.getInt("elite.xp", 12);
        eliteEfectos.clear();
        eliteEfectos.addAll(c.getStringList("elite.efectos"));
        eliteFuerza = c.getString("elite.efecto-fuerza", "strength:1");
        eliteOcultarParticulas = c.getBoolean("elite.ocultar-particulas", false);
        eliteBrillo = c.getBoolean("elite.brillo", false);
        eliteNombre = c.getString("elite.nombre", "");
        venganzaProb = c.getDouble("elite.venganza.prob", 35);
        venganzaCantidad = Math.max(0, c.getInt("elite.venganza.cantidad", 2));

        dropChance = c.getDouble("equipo.drop-chance", 0.085);
        loadTiers(c, "equipo.armaduras", armaduras);
        loadTiers(c, "equipo.cuerpo", cuerpo);
        loadTiers(c, "equipo.arco", arco);

        hordaActiva = c.getBoolean("horda.activa", true);
        hordaMinutos = Math.max(0.05, c.getDouble("horda.cada-minutos", 6));
        hordaProb = c.getDouble("horda.prob", 35);
        hordaInicio = c.getLong("horda.tiempo-inicio", 12000);
        hordaFin = c.getLong("horda.tiempo-fin", 23000);
        hordaYMin = c.getInt("horda.y-minima", 50);
        hordaRadio = Math.max(1, c.getInt("horda.radio", 20));
        hordaDistMin = Math.max(0, c.getInt("horda.distancia-minima", 8));
        hordaMin = Math.max(1, c.getInt("horda.cantidad-min", 3));
        hordaMax = Math.max(hordaMin, c.getInt("horda.cantidad-max", 5));
        hordaSoloCielo = c.getBoolean("horda.solo-cielo-abierto", false);
        hordaIgnorar.clear();
        for (String g : c.getStringList("horda.ignorar-modos")) {
            try {
                hordaIgnorar.add(GameMode.valueOf(g.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ex) {
                log.warning("horda.ignorar-modos: modo desconocido '" + g + "'");
            }
        }
        loadWeights(c.getConfigurationSection("horda.mobs"), hordaMobs, "horda.mobs");

        camaActiva = c.getBoolean("cama.activa", true);
        camaProb = c.getDouble("cama.prob", 25);
        camaRadio = Math.max(1, c.getInt("cama.radio", 6));
        camaDistMin = Math.max(0, c.getInt("cama.distancia-minima", 2));
        camaCantidad = Math.max(0, c.getInt("cama.cantidad", 2));
        loadWeights(c.getConfigurationSection("cama.mobs"), camaMobs, "cama.mobs");

        ghastActivo = c.getBoolean("ghast.activo", true);
        ghastRadio = c.getDouble("ghast.radio-explosion", 2.0);
        ghastSoloGhast = c.getBoolean("ghast.solo-ghast", true);

        telaranaAlRomper = null;
        ConfigurationSection tel = c.getConfigurationSection("telarana.al-romper");
        if (tel != null) {
            Ability ab = new Ability("telarana-al-romper");
            ab.chance = tel.getDouble("prob", 35);
            for (String a : tel.getStringList("acciones")) {
                addAction(ab, a, "telarana.al-romper");
            }
            telaranaAlRomper = ab;
        }

        medidorRetraso = Math.max(0, c.getInt("medidor.retraso-ticks", 40));
        autoguardadoMin = Math.max(1, c.getInt("autoguardado-minutos", 5));

        loadMobs();
    }

    // ------------------------------------------------------------------
    private void loadWeights(ConfigurationSection sec, Map<String, Integer> out, String where) {
        out.clear();
        if (sec == null) {
            return;
        }
        for (String k : sec.getKeys(false)) {
            EntityType t = Util.entityType(k);
            if (!Util.isSummonable(t)) {
                log.warning(where + ": '" + k + "' no es un mob invocable.");
                continue;
            }
            out.put(k.toLowerCase(Locale.ROOT), Math.max(0, sec.getInt(k)));
        }
    }

    private void loadTiers(FileConfiguration c, String path, List<GearTier> out) {
        out.clear();
        for (Map<?, ?> m : c.getMapList(path)) {
            GearTier tier = new GearTier((int) Util.num(m.get("prob"), 0));
            for (Map.Entry<?, ?> en : m.entrySet()) {
                String key = String.valueOf(en.getKey());
                if (key.equals("prob")) {
                    continue;
                }
                EquipmentSlot slot = Util.slotFor(key);
                if (slot == null) {
                    log.warning(path + ": ranura desconocida '" + key + "'");
                    continue;
                }
                ItemStack item = Util.parseItem(String.valueOf(en.getValue()));
                if (item == null) {
                    log.warning(path + ": item invalido '" + en.getValue() + "'");
                    continue;
                }
                tier.items.put(slot, item);
            }
            out.add(tier);
        }
    }

    private void loadMobs() {
        mobs.clear();
        presets.clear();
        YamlConfiguration y = Util2.loadYaml(plugin, "mobs.yml");

        ConfigurationSection ps = y.getConfigurationSection("presets");
        if (ps != null) {
            for (String id : ps.getKeys(false)) {
                ConfigurationSection s = ps.getConfigurationSection(id);
                if (s == null) {
                    continue;
                }
                Preset p = new Preset();
                for (String k : s.getKeys(false)) {
                    if (k.equalsIgnoreCase("vida")) {
                        p.vida = s.getDouble(k);
                        continue;
                    }
                    EquipmentSlot slot = Util.slotFor(k);
                    ItemStack item = Util.parseItem(s.getString(k));
                    if (slot == null || item == null) {
                        log.warning("presets." + id + "." + k + ": ranura o item invalido");
                        continue;
                    }
                    p.items.put(slot, item);
                }
                presets.put(id.toLowerCase(Locale.ROOT), p);
            }
        }

        ConfigurationSection ms = y.getConfigurationSection("mobs");
        if (ms == null) {
            log.warning("mobs.yml no tiene la seccion 'mobs'.");
            return;
        }
        for (String id : ms.getKeys(false)) {
            ConfigurationSection s = ms.getConfigurationSection(id);
            if (s == null) {
                continue;
            }
            String key = id.toLowerCase(Locale.ROOT);
            if (Util.entityType(key) == null) {
                log.warning("mobs.yml: '" + id + "' no es un tipo de entidad valido; se ignora.");
                continue;
            }
            try {
                mobs.put(key, parseMob(key, s));
            } catch (RuntimeException ex) {
                log.warning("mobs.yml: error en '" + id + "': " + ex.getMessage());
            }
        }
    }

    private MobProfile parseMob(String id, ConfigurationSection s) {
        MobProfile p = new MobProfile(id);
        p.vida = multiplier(s.get("vida"), baseVida);
        p.dano = multiplier(s.get("dano"), baseDano);
        p.entorno = Util.bool(s.get("entorno"), p.dano != null);
        p.equipo = s.getString("equipo", "ninguno").toLowerCase(Locale.ROOT);
        if (!p.equipo.equals("cuerpo") && !p.equipo.equals("arco") && !p.equipo.equals("ninguno")) {
            log.warning("mobs.yml: " + id + ".equipo '" + p.equipo + "' invalido (cuerpo|arco|ninguno)");
            p.equipo = "ninguno";
        }
        ConfigurationSection el = s.getConfigurationSection("elite");
        if (el != null) {
            p.eliteProb = el.getDouble("prob", 0);
            p.eliteFuerza = el.getBoolean("fuerza", false);
        }
        p.cargadoProb = s.getDouble("cargado", 0);
        ConfigurationSection sub = s.getConfigurationSection("subterraneo");
        if (sub != null) {
            p.subterraneoY = sub.getDouble("y", 0);
            p.subterraneoExtra = sub.getDouble("radio-extra", 1.0);
        }
        ConfigurationSection gr = s.getConfigurationSection("grupo");
        if (gr != null) {
            p.grupoProb = gr.getDouble("prob", 0);
            p.grupoCantidad = Math.max(1, gr.getInt("cantidad", 1));
        }
        loadAbilities(s, "al-aparecer", id, p.alAparecer);
        loadAbilities(s, "al-golpear", id, p.alGolpear);
        loadAbilities(s, "al-morir", id, p.alMorir);
        return p;
    }

    private Double multiplier(Object v, double global) {
        if (v instanceof Boolean b) {
            return b ? global : null;
        }
        if (v instanceof Number n) {
            return n.doubleValue();
        }
        return null;
    }

    private void loadAbilities(ConfigurationSection s, String path, String mobId, List<Ability> out) {
        int index = 0;
        for (Map<?, ?> m : s.getMapList(path)) {
            index++;
            Object rawId = m.get("id");
            String abId = rawId == null ? "habilidad" + index : String.valueOf(rawId);
            Ability ab = new Ability(abId);
            ab.chance = Util.num(m.get("prob"), 100.0);
            ab.scaleChance = Util.bool(m.get("escalar-prob"), true);
            ab.cooldown = Util.num(m.get("cooldown"), 0.0);
            Object proj = m.get("proyectil");
            if (proj != null) {
                ab.projectile = Util.bool(proj, false);
            }
            Object only = m.get("solo-tipo");
            if (only != null) {
                ab.onlyTipo = String.valueOf(only).toLowerCase(Locale.ROOT);
            }
            Object except = m.get("excepto-tipo");
            if (except != null) {
                ab.exceptTipo = String.valueOf(except).toLowerCase(Locale.ROOT);
            }
            Object acts = m.get("acciones");
            if (acts instanceof List<?> list) {
                for (Object o : list) {
                    addAction(ab, String.valueOf(o), mobId + "." + path + "." + abId);
                }
            }
            if (ab.actions.isEmpty()) {
                log.warning("mobs.yml: " + mobId + "." + path + "." + abId + " no tiene acciones validas.");
            }
            out.add(ab);
        }
    }

    private void addAction(Ability ab, String raw, String where) {
        Action a = Action.parse(raw);
        String err = plugin.actions().validate(a);
        if (err != null) {
            log.warning(where + ": accion '" + raw + "' ignorada (" + err + ")");
            return;
        }
        ab.actions.add(a);
    }
}
