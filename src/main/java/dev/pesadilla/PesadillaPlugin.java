package dev.pesadilla;

import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Modo Pesadilla: dificultad extrema y habilidades de mobs. */
public final class PesadillaPlugin extends JavaPlugin {

    private Settings settings;
    private Msg msg;
    private DataStore data;
    private LevelManager levels;
    private Cooldowns cooldowns;
    private Actions actions;
    private MobService mobs;
    private EnvironmentFactor env;
    private HordeManager horde;
    private NamespacedKey tipoKey;
    private BukkitTask autosave;
    private final Set<UUID> debugPlayers = ConcurrentHashMap.newKeySet();

    @Override
    public void onEnable() {
        saveDefaultConfig();
        tipoKey = new NamespacedKey(this, "tipo");

        msg = new Msg(this);
        data = new DataStore(this);
        cooldowns = new Cooldowns();
        actions = new Actions(this);
        settings = new Settings(this);
        levels = new LevelManager(this);
        mobs = new MobService(this);
        env = new EnvironmentFactor(this);
        horde = new HordeManager(this);

        data.load();
        reloadAll();

        getServer().getPluginManager().registerEvents(new SpawnListener(this), this);
        getServer().getPluginManager().registerEvents(new CombatListener(this), this);
        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new WorldListener(this), this);

        PesadillaCommand admin = new PesadillaCommand(this);
        bind("pesadilla", admin, admin);
        PlayerCommands player = new PlayerCommands(this);
        bind("medidor", player, player);

        getLogger().info("Modo Pesadilla activo. Nivel: " + levels.name()
                + " | mobs: " + settings.mobs.size());
    }

    private void bind(String name, CommandExecutor exec, TabCompleter tab) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) {
            getLogger().severe("Comando '" + name + "' no definido en plugin.yml");
            return;
        }
        cmd.setExecutor(exec);
        cmd.setTabCompleter(tab);
    }

    /** Recarga todos los archivos de configuracion y reinicia las tareas. */
    public void reloadAll() {
        msg.load();
        settings.load();
        levels.load();
        horde.start();
        if (autosave != null) {
            autosave.cancel();
        }
        long period = Math.max(1L, settings.autoguardadoMin) * 60L * 20L;
        autosave = getServer().getScheduler().runTaskTimer(this, () -> data.save(), period, period);
    }

    @Override
    public void onDisable() {
        if (autosave != null) {
            autosave.cancel();
        }
        if (horde != null) {
            horde.stop();
        }
        if (actions != null) {
            actions.clearWebs();
        }
        if (data != null) {
            data.markDirty();
            data.save();
        }
    }

    public Settings settings() {
        return settings;
    }

    public Msg msg() {
        return msg;
    }

    public DataStore data() {
        return data;
    }

    public LevelManager levels() {
        return levels;
    }

    public Cooldowns cooldowns() {
        return cooldowns;
    }

    public Actions actions() {
        return actions;
    }

    public MobService mobs() {
        return mobs;
    }

    public EnvironmentFactor env() {
        return env;
    }

    public HordeManager horde() {
        return horde;
    }

    public NamespacedKey tipoKey() {
        return tipoKey;
    }

    public Set<UUID> debugPlayers() {
        return debugPlayers;
    }
}
