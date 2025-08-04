package org.rexi.velocityUtilsLink;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class VelocityUtilsLink extends JavaPlugin implements Listener, PluginMessageListener {

    private final Map<UUID, String> pendingMessages = new HashMap<>();
    private final java.util.Set<UUID> ignoreNextChatEvent = new java.util.HashSet<>();

    private VelocityUtilsPlaceholders expansion;

    @Override
    public void onEnable() {
        //Staffchat & Adminchat

        Bukkit.getPluginManager().registerEvents(this, this);

        // Registrar canales de plugin messaging
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "velocityutils:staffchat");
        Bukkit.getMessenger().registerIncomingPluginChannel(this, "velocityutils:staffchat", this);

        Bukkit.getMessenger().registerOutgoingPluginChannel(this, "velocityutils:adminchat");
        Bukkit.getMessenger().registerIncomingPluginChannel(this, "velocityutils:adminchat", this);

        //Placeholders

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null &&
                Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {

            this.expansion = new VelocityUtilsPlaceholders(this);
            expansion.register();

            Bukkit.getMessenger().registerIncomingPluginChannel(this, "velocityutils:placeholders", expansion);
            Bukkit.getMessenger().registerOutgoingPluginChannel(this, "velocityutils:placeholders");

            expansion.startAutoRefresh();

            getLogger().info("PlaceholderAPI detectaded.");
        } else {
            getLogger().warning("PlaceholderAPI not detected.");
        }
    }

    @EventHandler
    public void onAsyncPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (ignoreNextChatEvent.remove(uuid)) {
            return;
        }

        boolean hasStaff = player.hasPermission("velocityutils.staffchat");
        boolean hasAdmin = player.hasPermission("velocityutils.adminchat");

        if (!hasStaff && !hasAdmin) {
            return; // No tiene permisos especiales
        }

        // Cancelamos el mensaje original por si va a staffchat o adminchat
        event.setCancelled(true);

        pendingMessages.put(uuid, event.getMessage());

        if (hasAdmin) {
            requestToggleStatus(player, event.getMessage(), "adminchat");
        } else if (hasStaff) {
            requestToggleStatus(player, event.getMessage(), "staffchat");
        }
    }

    private void requestToggleStatus(Player player, String message, String channelName) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(out);

            data.writeUTF("toggle_request");
            data.writeUTF(player.getUniqueId().toString());
            data.writeUTF(message);

            player.sendPluginMessage(this, "velocityutils:" + channelName, out.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("velocityutils:staffchat") && !channel.equals("velocityutils:adminchat")) return;

        String type = channel.endsWith("adminchat") ? "adminchat" : "staffchat";

        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            String subChannel = in.readUTF();

            if (subChannel.equals("toggle_response")) {
                UUID playerUUID = UUID.fromString(in.readUTF());
                boolean toggled = in.readBoolean();
                String msg = in.readUTF();

                Player p = Bukkit.getPlayer(playerUUID);
                if (p == null) return;

                pendingMessages.remove(playerUUID); // Ya lo hemos procesado

                if (type.equals("adminchat")) {
                    if (toggled) {
                        sendToProxy(p, msg, "adminchat");
                    } else if (p.hasPermission("velocityutils.staffchat")) {
                        requestToggleStatus(p, msg, "staffchat");
                    } else {
                        sendToNormalChat(p, msg);
                    }
                } else if (type.equals("staffchat")) {
                    if (toggled) {
                        sendToProxy(p, msg, "staffchat");
                    } else {
                        sendToNormalChat(p, msg);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void sendToNormalChat(Player player, String msg) {
        UUID uuid = player.getUniqueId();

        ignoreNextChatEvent.add(uuid); // evitar que se vuelva a procesar

        Bukkit.getScheduler().runTask(this, () -> {
            player.chat(msg); // esto dispara de nuevo el evento, pero será ignorado
        });
    }


    private void sendToProxy(Player player, String message, String channelName) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(out);

            data.writeUTF(channelName);
            data.writeUTF(player.getUniqueId().toString());
            data.writeUTF(player.getName());
            data.writeUTF(message);

            player.sendPluginMessage(this, "velocityutils:" + channelName, out.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
