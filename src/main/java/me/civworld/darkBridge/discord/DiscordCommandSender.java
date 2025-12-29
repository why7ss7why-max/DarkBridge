package me.civworld.darkBridge.discord;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class DiscordCommandSender implements CommandSender {

    private final List<String> messages = new ArrayList<>();
    private final Plugin plugin;
    private final ConsoleCommandSender console;

    public DiscordCommandSender(Plugin plugin) {
        this.plugin = plugin;
        this.console = Bukkit.getConsoleSender();
    }

    public String getResult() {
        return messages.isEmpty() ? "Команда выполнена без вывода." : String.join("\n", messages);
    }

    @Override
    public void sendMessage(@NotNull String message) {
        String clean = message.replaceAll("§[0-9a-fk-or]", "");
        messages.add(clean);
    }

    @Override
    public void sendMessage(@NotNull String... messages) {
        for (String msg : messages) {
            sendMessage(msg);
        }
    }

    @Override
    public void sendMessage(@Nullable UUID uuid, @NotNull String message) {
        sendMessage(message);
    }

    @Override
    public void sendMessage(@Nullable UUID uuid, @NotNull String... messages) {
        sendMessage(messages);
    }

    // Делегируем методы Permission к консоли
    @Override
    public boolean hasPermission(@NotNull String name) {
        return true; // Discord команды выполняются с правами OP
    }

    @Override
    public boolean hasPermission(@NotNull Permission perm) {
        return true;
    }

    @Override
    public boolean isPermissionSet(@NotNull String name) {
        return console.isPermissionSet(name);
    }

    @Override
    public boolean isPermissionSet(@NotNull Permission perm) {
        return console.isPermissionSet(perm);
    }

    @Override
    public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin) {
        return console.addAttachment(plugin);
    }

    @Override
    public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, int ticks) {
        return console.addAttachment(plugin, ticks);
    }

    @Override
    public @NotNull PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value) {
        return console.addAttachment(plugin, name, value);
    }

    @Override
    public @Nullable PermissionAttachment addAttachment(@NotNull Plugin plugin, @NotNull String name, boolean value, int ticks) {
        return console.addAttachment(plugin, name, value, ticks);
    }

    @Override
    public void removeAttachment(@NotNull PermissionAttachment attachment) {
        console.removeAttachment(attachment);
    }

    @Override
    public void recalculatePermissions() {
        console.recalculatePermissions();
    }

    @Override
    public @NotNull Set<PermissionAttachmentInfo> getEffectivePermissions() {
        return console.getEffectivePermissions();
    }

    @Override
    public boolean isOp() {
        return true; // Discord команды выполняются как OP
    }

    @Override
    public void setOp(boolean value) {
        // Игнорируем
    }

    @Override
    public @NotNull String getName() {
        return "Discord";
    }

    @Override
    public @NotNull Component name() {
        return Component.text("Discord");
    }

    @Override
    public @NotNull Server getServer() {
        return plugin.getServer();
    }

    @Override
    public @NotNull Spigot spigot() {
        return console.spigot();
    }
}