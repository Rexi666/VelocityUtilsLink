package org.rexi.velocityUtilsLink.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.rexi.velocityUtilsLink.VelocityUtilsLink;

public class LegacyChatListener implements Listener {

    private final VelocityUtilsLink plugin;

    public LegacyChatListener(VelocityUtilsLink plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onLegacyChat(AsyncPlayerChatEvent event) {
        plugin.handleChat(event.getPlayer(), event.getMessage(), event);
    }
}