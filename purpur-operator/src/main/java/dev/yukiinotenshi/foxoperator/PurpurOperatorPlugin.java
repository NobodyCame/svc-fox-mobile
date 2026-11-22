package dev.yukiinotenshi.foxoperator;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

public final class PurpurOperatorPlugin extends JavaPlugin implements Listener {
    private LocalOperator operator;

    @Override public void onEnable() {
        if (getClass().getResource("/fox-operator/operator_stdio.py") == null) {
            getLogger().warning("Local operator backend is missing from this JAR.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        saveDefaultConfig();
        var directory = getDataFolder().toPath();
        operator = new LocalOperator(directory, directory.resolve("interpreter"), getConfig().getString("python", "auto"),
                message -> getLogger().warning(message));
        var messenger = getServer().getMessenger();
        messenger.registerOutgoingPluginChannel(this, OperatorWire.CHANNEL);
        messenger.registerIncomingPluginChannel(this, OperatorWire.CHANNEL, (channel, player, bytes) ->
                operator.receive(player, player.getUniqueId(), player.getName(), bytes,
                        task -> getServer().getScheduler().runTask(this, task),
                        response -> {
                            if (player.isOnline()) player.sendPluginMessage(this, OperatorWire.CHANNEL, response);
                        }));
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler public void onQuit(PlayerQuitEvent event) {
        operator.disconnect(event.getPlayer());
    }

    @Override public void onDisable() {
        if (operator != null) operator.close();
    }
}
