package org.rexi.velocityUtilsLink.commands;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageRecipient;
import org.bukkit.plugin.java.JavaPlugin;

public class vualertCommand implements CommandExecutor {

    private final JavaPlugin plugin;

    public vualertCommand(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!sender.hasPermission("velocityutils.alert")) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&cYou dont have permission to use that command"));
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize("&c/vualert <alert>"));
            return false;
        }

        String message = String.join(" ", args);

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(message);

        // Enviar el mensaje usando cualquier jugador conectado
        Player player = Bukkit.getOnlinePlayers().stream().findFirst().orElse(null);
        if (player == null) {
            sender.sendMessage("§cNo conected players to send the alert.");
            return true;
        }

        ((PluginMessageRecipient) player).sendPluginMessage(plugin, "velocityutils:alerts", out.toByteArray());
        Bukkit.getConsoleSender().sendMessage(message);
        return true;
    }
}
