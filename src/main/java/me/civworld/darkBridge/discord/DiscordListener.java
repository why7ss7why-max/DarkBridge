package me.civworld.darkBridge.discord;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.InteractionHook;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class DiscordListener extends ListenerAdapter {

    private final Plugin plugin;
    private final String roleId;

    public DiscordListener(Plugin plugin, String roleId){
        this.plugin = plugin;
        this.roleId = roleId;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (!event.getName().equals("cmd")) return;

        plugin.getLogger().info("=== DISCORD COMMAND RECEIVED ===");

        var member = event.getMember();
        if (member == null) {
            plugin.getLogger().warning("Member is null!");
            event.reply("❌ Произошла ошибка").setEphemeral(true).queue();
            return;
        }

        plugin.getLogger().info("User: " + member.getEffectiveName() + " (" + member.getId() + ")");

        if (member.getRoles().stream().noneMatch(r -> r.getId().equals(roleId))) {
            plugin.getLogger().warning("User doesn't have required role: " + roleId);
            event.reply("❌ У вас нет прав для выполнения этой команды").setEphemeral(true).queue();
            return;
        }

        var option = event.getOption("command");
        if (option == null) {
            plugin.getLogger().warning("Command option is null!");
            event.reply("❌ Команда не указана").setEphemeral(true).queue();
            return;
        }

        String cmd = option.getAsString();

        // Убираем начальный "/" если он есть
        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }

        plugin.getLogger().info("Command to execute: '" + cmd + "'");
        plugin.getLogger().info("Deferring reply immediately...");

        final String finalCmd = cmd;

        InteractionHook hook;
        try {
            // ПУБЛИЧНЫЙ ответ (все видят)
            hook = event.deferReply(false).complete();
            plugin.getLogger().info("Reply deferred successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to defer reply: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        AtomicBoolean responseSent = new AtomicBoolean(false);

        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            if (!responseSent.getAndSet(true)) {
                plugin.getLogger().info("Timeout reached, sending timeout message...");

                EmbedBuilder timeoutEmbed = new EmbedBuilder()
                        .setTitle("⏱️ Команда отправлена")
                        .addField("Команда", "`" + finalCmd + "`", false)
                        .setDescription("Команда выполняется, но время ожидания ответа истекло.\nРезультат будет виден в консоли сервера.")
                        .setColor(Color.ORANGE)
                        .setFooter("Выполнил <@" + member.getId() + ">");

                try {
                    hook.sendMessageEmbeds(timeoutEmbed.build()).complete();
                    plugin.getLogger().info("Timeout message sent successfully");
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to send timeout message: " + e.getMessage());
                }
            }
        }, 50L);

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                plugin.getLogger().info("Executing command in main thread...");

                DiscordCommandSender sender = new DiscordCommandSender(plugin);
                boolean success = Bukkit.dispatchCommand(sender, finalCmd);
                String result = sender.getResult();

                plugin.getLogger().info("Command executed. Success: " + success);
                plugin.getLogger().info("Result length: " + result.length() + " chars");

                if (!responseSent.getAndSet(true)) {
                    plugin.getLogger().info("Sending result to Discord...");

                    if (result.length() > 3900) {
                        result = result.substring(0, 3900) + "\n... (обрезано)";
                    }

                    EmbedBuilder embed = new EmbedBuilder()
                            .setTitle("✅ Результат команды")
                            .addField("Команда", "`" + finalCmd + "`", false)
                            .setDescription("```\n" + result + "\n```")
                            .setColor(Color.GREEN)
                            .setFooter("Выполнил <@" + member.getId() + ">");

                    try {
                        hook.sendMessageEmbeds(embed.build()).complete();
                        plugin.getLogger().info("Result sent successfully");
                    } catch (Exception e) {
                        plugin.getLogger().warning("Failed to send result: " + e.getMessage());
                    }
                } else {
                    plugin.getLogger().info("Command completed after timeout. Result: " + result);
                }

            } catch (IllegalArgumentException e) {
                // Ошибка "Cannot make ... a vanilla command listener"
                plugin.getLogger().warning("Vanilla command compatibility issue: " + e.getMessage());

                if (!responseSent.getAndSet(true)) {
                    EmbedBuilder errorEmbed = new EmbedBuilder()
                            .setTitle("❌ Команда не найдена")
                            .addField("Команда", "`" + finalCmd + "`", false)
                            .setDescription("Команда не найдена или недоступна через Discord.\nПроверьте правильность написания команды.")
                            .setColor(Color.RED)
                            .setFooter("Выполнил <@" + member.getId() + ">");

                    try {
                        hook.sendMessageEmbeds(errorEmbed.build()).complete();
                    } catch (Exception sendError) {
                        plugin.getLogger().severe("Failed to send error message: " + sendError.getMessage());
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().severe("Error executing command: " + e.getMessage());
                e.printStackTrace();

                if (!responseSent.getAndSet(true)) {
                    EmbedBuilder errorEmbed = new EmbedBuilder()
                            .setTitle("❌ Ошибка выполнения")
                            .addField("Команда", "`" + finalCmd + "`", false)
                            .setDescription("```\n" + e.getMessage() + "\n```")
                            .setColor(Color.RED)
                            .setFooter("Выполнил <@" + member.getId() + ">");

                    try {
                        hook.sendMessageEmbeds(errorEmbed.build()).complete();
                    } catch (Exception sendError) {
                        plugin.getLogger().severe("Failed to send error message: " + sendError.getMessage());
                    }
                }
            }
        });

        plugin.getLogger().info("=== DISCORD COMMAND PROCESSING STARTED ===");
    }
}