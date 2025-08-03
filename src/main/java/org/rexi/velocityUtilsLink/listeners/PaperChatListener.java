package org.rexi.velocityUtilsLink.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.rexi.velocityUtilsLink.VelocityUtilsLink;

public class PaperChatListener implements Listener {

    private final VelocityUtilsLink plugin;

    public PaperChatListener(VelocityUtilsLink plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPaperChat(AsyncChatEvent event) {
        String msg = LegacyComponentSerializer.legacySection().serialize(event.message());
        plugin.handleChat(event.getPlayer(), msg, event);
    }
}