package dev.pesadilla;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;

/** Datos persistentes (data.yml): nivel y medidores. */
public final class DataStore {

    private final PesadillaPlugin plugin;
    private final File file;
    private YamlConfiguration yml = new YamlConfiguration();
    private boolean dirty = false;

    public DataStore(PesadillaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
    }

    public void load() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("No se pudo crear la carpeta del plugin.");
        }
        yml = YamlConfiguration.loadConfiguration(file);
        dirty = false;
    }

    public synchronized void save() {
        if (!dirty) {
            return;
        }
        try {
            yml.save(file);
            dirty = false;
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "No se pudo guardar data.yml", ex);
        }
    }

    public YamlConfiguration yml() {
        return yml;
    }

    public void markDirty() {
        dirty = true;
    }

    // ---- nivel ----
    public String getLevel() {
        return yml.getString("nivel");
    }

    public void setLevel(String level) {
        yml.set("nivel", level);
        markDirty();
    }

    // ---- medidor ----
    public boolean hasMedidor(UUID id) {
        return yml.getStringList("medidor").contains(id.toString());
    }

    /** Alterna el medidor y devuelve el nuevo estado. */
    public boolean toggleMedidor(UUID id) {
        List<String> list = yml.getStringList("medidor");
        Set<String> set = new HashSet<>(list);
        boolean now;
        if (set.contains(id.toString())) {
            set.remove(id.toString());
            now = false;
        } else {
            set.add(id.toString());
            now = true;
        }
        yml.set("medidor", new java.util.ArrayList<>(set));
        markDirty();
        return now;
    }
}
