package me.civworld.darkBridge.config;

import org.bukkit.plugin.Plugin;

public class Config {

    private final Plugin plugin;

    public Config(Plugin plugin){
        this.plugin = plugin;
    }

    public String getRoleId(){
        plugin.saveDefaultConfig();
        return plugin.getConfig().getString("roleId", "");
    }

    public String getToken(){
        plugin.saveDefaultConfig();
        return plugin.getConfig().getString("token", "");
    }
}