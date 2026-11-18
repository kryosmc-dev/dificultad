package dev.pesadilla;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/** /medidor (comando de jugadores). */
public final class PlayerCommands implements CommandExecutor, TabCompleter {

    private final PesadillaPlugin plugin;

    public PlayerCommands(PesadillaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Msg msg = plugin.msg();

        if (!sender.hasPermission("pesadilla.medidor")) {
            msg.send(sender, "sin-permiso");
            return true;
        }
        if (!(sender instanceof Player p)) {
            msg.send(sender, "solo-jugadores");
            return true;
        }
        LevelManager lv = plugin.levels();
        if (args.length > 0 && args[0].equalsIgnoreCase("ahora")) {
            msg.send(p, "medidor-ahora-nivel", "nivel", lv.name(), "factor", String.valueOf(Util.round2(lv.factor())));
            double env = plugin.env().factor(p);
            msg.send(p, "medidor-ahora-entorno", "valor", String.valueOf(Util.round2(env)));
            msg.send(p, "medidor-ahora-dano", "valor",
                    String.valueOf(Util.round2(lv.scale(plugin.settings().baseDano) * env)));
            return true;
        }
        if (plugin.data().toggleMedidor(p.getUniqueId())) {
            msg.send(p, "medidor-on");
            msg.send(p, "medidor-on-ayuda");
        } else {
            msg.send(p, "medidor-off");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();
        if (args.length == 1
                && "ahora".startsWith(args[0].toLowerCase())) {
            out.add("ahora");
        }
        return out;
    }
}
