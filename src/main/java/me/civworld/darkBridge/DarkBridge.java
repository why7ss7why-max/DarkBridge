package me.civworld.darkBridge;

import me.civworld.darkBridge.config.Config;
import me.civworld.darkBridge.discord.DiscordManager;
import org.bukkit.plugin.java.JavaPlugin;

import static ru.civworld.darkAPI.DarkAPI.*;

public final class DarkBridge extends JavaPlugin {
    private DiscordManager discordManager;

    @Override
    public void onEnable() {
        registerPlugin(this, "<gray>[<red>DarkBridge<gray>] <white>");

        log("Enabling plugin...");

        Config config = new Config(this);
        log("Loading config...");

        String roleId = config.getRoleId();
        String token = config.getToken();

        if (token.isEmpty()) {
            error("Token is empty! Please configure the plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        log("Creating DiscordManager");
        discordManager = new DiscordManager(this, token, roleId);

        log("Initializing DiscordManager (async)");
        discordManager.initialize();

        log("Plugin successfully enabled!");
    }

    @Override
    public void onDisable() {
        if (discordManager != null) {
            log("Shutting down Discord connection...");
            discordManager.shutdown();
        }
        log("Plugin successfully disabled!");
    }
}