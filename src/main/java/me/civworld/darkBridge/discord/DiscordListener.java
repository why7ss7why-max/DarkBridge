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

import static ru.civworld.darkAPI.DarkAPI.error;
import static ru.civworld.darkAPI.DarkAPI.log;

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

        log("=== DISCORD COMMAND RECEIVED ===");

        var member = event.getMember();
        if (member == null) {
            error("Member is null!");
            event.reply("❌ Произошла ошибка").setEphemeral(true).queue();
            return;
        }

        log("User: " + member.getEffectiveName() + " (" + member.getId() + ")");

        if (member.getRoles().stream().noneMatch(r -> r.getId().equals(roleId))) {
            error("User doesn't have required role: " + roleId);
            event.reply("❌ У вас нет прав для выполнения этой команды").setEphemeral(true).queue();
            return;
        }

        var option = event.getOption("command");
        if (option == null) {
            error("Command option is null!");
            event.reply("❌ Команда не указана").setEphemeral(true).queue();
            return;
        }

        String cmd = option.getAsString();

        if (cmd.startsWith("/")) {
            cmd = cmd.substring(1);
        }

        log("Command to execute: '" + cmd + "'");
        log("Deferring reply immediately...");

        final String finalCmd = cmd;

        InteractionHook hook;
        try {
            hook = event.deferReply(false).complete();
            log("Reply deferred successfully!");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to defer reply: " + e.getMessage());
            e.printStackTrace();
            return;
        }

        AtomicBoolean responseSent = new AtomicBoolean(false);

        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            if (!responseSent.getAndSet(true)) {
                log("Timeout reached, sending timeout message...");

                EmbedBuilder timeoutEmbed = new EmbedBuilder()
                        .setTitle("⏱️ Команда отправлена")
                        .addField("Команда", "`" + finalCmd + "`", false)
                        .setDescription("Команда выполняется, но время ожидания ответа истекло.\nРезультат будет виден в консоли сервера.")
                        .setColor(Color.ORANGE)
                        .setFooter("Выполнил <@" + member.getId() + ">");

                try {
                    hook.sendMessageEmbeds(timeoutEmbed.build()).complete();
                    log("Timeout message sent successfully");
                } catch (Exception e) {
                    error("Failed to send timeout message: " + e.getMessage());
                }
            }
        }, 50L);

        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                log("Executing command in main thread...");

                DiscordCommandSender sender = new DiscordCommandSender(plugin);
                boolean success = Bukkit.dispatchCommand(sender, finalCmd);
                String result = sender.getResult();

                log("Command executed. Success: " + success);
                log("Result length: " + result.length() + " chars");

                if (!responseSent.getAndSet(true)) {
                    log("Sending result to Discord...");

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
                        log("Result sent successfully");
                    } catch (Exception e) {
                        error("Failed to send result: " + e.getMessage());
                    }
                } else {
                    log("Command completed after timeout. Result: " + result);
                }

            } catch (IllegalArgumentException e) {
                error("Vanilla command compatibility issue: " + e.getMessage());

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

        log("=== DISCORD COMMAND PROCESSING STARTED ===");
    }
}