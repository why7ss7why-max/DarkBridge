package me.civworld.darkBridge.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bukkit.plugin.Plugin;

import static ru.civworld.darkAPI.DarkAPI.error;
import static ru.civworld.darkAPI.DarkAPI.log;

public class DiscordManager {
    private final Plugin plugin;
    private JDA jda;
    private final String token;
    private final String roleId;

    public DiscordManager(Plugin plugin, String token, String roleId){
        this.plugin = plugin;
        this.token = token;
        this.roleId = roleId;
    }

    public void initialize(){
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                log("Starting JDA initialization...");

                this.jda = JDABuilder.createDefault(token)
                        .enableIntents(
                                GatewayIntent.GUILD_MESSAGES,
                                GatewayIntent.MESSAGE_CONTENT
                        )
                        .setActivity(Activity.listening("/cmd"))
                        .addEventListeners(new DiscordListener(plugin, roleId))
                        .build();

                log("Waiting for JDA to be ready...");
                jda.awaitReady();

                log("JDA is ready! Bot is: " + jda.getSelfUser().getAsTag());
                log("Registering slash commands...");

                jda.updateCommands()
                        .addCommands(
                                Commands.slash("cmd", "Отправить команду в консоль сервера")
                                        .addOption(
                                                OptionType.STRING,
                                                "command",
                                                "Команда для выполнения (без слеша)",
                                                true
                                        )
                        ).queue(
                                success -> {
                                    log("✓ Slash command registered successfully!");
                                },
                                error -> error("✗ Failed to register command: " + error.getMessage())
                        );

                log("Discord bot is fully operational!");

            } catch (InterruptedException e) {
                error("JDA initialization was interrupted!");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                error("Failed to initialize JDA: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void shutdown() {
        if (jda != null) {
            try {
                log("Shutting down JDA...");
                jda.getRegisteredListeners().forEach(jda::removeEventListener);
                jda.shutdownNow();
                log("JDA shutdown initiated");
            } catch (Exception e) {
                log("JDA shutdown completed with minor warnings (normal for hot reload)");
            }
        }
    }

    public JDA getJda() {
        return jda;
    }
}