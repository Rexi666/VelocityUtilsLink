package org.rexi.velocityUtilsLink;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class VelocityUtilsPlaceholders extends PlaceholderExpansion implements PluginMessageListener {

    private final Plugin plugin;
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<String>> pendingRequests = new ConcurrentHashMap<>();

    public VelocityUtilsPlaceholders(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getAuthor() {
        return "Rexi666";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String getIdentifier() {
        return "velocityutils";
    }

    private final java.util.Set<String> usedIdentifiers = ConcurrentHashMap.newKeySet();

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        String cacheKey = player.getUniqueId() + ":" + identifier;
        usedIdentifiers.add(identifier);

        if (cache.containsKey(cacheKey)) {
            return cache.get(cacheKey);
        }

        CompletableFuture<String> future = pendingRequests.get(cacheKey);
        if (future != null && !future.isDone()) {
            return "Loading...";
        }

        final CompletableFuture<String> newFuture = new CompletableFuture<>();
        pendingRequests.put(cacheKey, newFuture);

        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(player.getUniqueId().toString());
        out.writeUTF(identifier);

        player.sendPluginMessage(plugin, "velocityutils:placeholders", out.toByteArray());

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!newFuture.isDone()) newFuture.complete("N/A");
            pendingRequests.remove(cacheKey);
        }, 20L);

        return "Loading...";
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!channel.equals("velocityutils:placeholders")) return;

        ByteArrayDataInput in = ByteStreams.newDataInput(message);
        String identifier = in.readUTF();
        String value = in.readUTF();

        String cacheKey = player.getUniqueId() + ":" + identifier;
        cache.put(cacheKey, value);

        CompletableFuture<String> future = pendingRequests.remove(cacheKey);
        if (future != null) {
            future.complete(value);
        } else {
        }
    }

    public void startAutoRefresh() {
        plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : plugin.getServer().getOnlinePlayers()) {
                for (String identifier : usedIdentifiers) {
                    ByteArrayDataOutput out = ByteStreams.newDataOutput();
                    out.writeUTF(player.getUniqueId().toString());
                    out.writeUTF(identifier);
                    player.sendPluginMessage(plugin, "velocityutils:placeholders", out.toByteArray());
                }
            }
        }, 20L, 60L); // 60 ticks = cada 3 segundos
    }
}
