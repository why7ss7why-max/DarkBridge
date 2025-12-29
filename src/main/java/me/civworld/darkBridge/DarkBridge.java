package me.civworld.darkBridge;

import me.civworld.darkBridge.config.Config;
import me.civworld.darkBridge.discord.DiscordManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class DarkBridge extends JavaPlugin {
    private DiscordManager discordManager;

    @Override
    public void onEnable() {
        getLogger().info("Enabling plugin...");

        Config config = new Config(this);
        getLogger().info("Loading config...");

        String roleId = config.getRoleId();
        String token = config.getToken();

        if (token.isEmpty()) {
            getLogger().severe("Token is empty! Please configure the plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("Creating DiscordManager");
        discordManager = new DiscordManager(this, token, roleId);

        getLogger().info("Initializing DiscordManager (async)");
        discordManager.initialize();

        getLogger().info("Plugin successfully enabled!");
    }

    @Override
    public void onDisable() {
        if (discordManager != null) {
            getLogger().info("Shutting down Discord connection...");
            discordManager.shutdown();
        }
        getLogger().info("Plugin successfully disabled!");
    }
}