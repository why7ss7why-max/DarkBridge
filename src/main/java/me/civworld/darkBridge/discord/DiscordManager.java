package me.civworld.darkBridge.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.bukkit.plugin.Plugin;

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
                plugin.getLogger().info("Starting JDA initialization...");

                this.jda = JDABuilder.createDefault(token)
                        .enableIntents(
                                GatewayIntent.GUILD_MESSAGES,
                                GatewayIntent.MESSAGE_CONTENT
                        )
                        .addEventListeners(new DiscordListener(plugin, roleId))
                        .build();

                plugin.getLogger().info("Waiting for JDA to be ready...");
                jda.awaitReady();

                plugin.getLogger().info("JDA is ready! Bot is: " + jda.getSelfUser().getAsTag());
                plugin.getLogger().info("Registering slash commands...");

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
                                    plugin.getLogger().info("✓ Slash command registered successfully!");
                                    plugin.getLogger().info("Command may take up to 1 hour to appear globally.");
                                    plugin.getLogger().info("For instant testing, add the command to your server directly.");
                                },
                                error -> plugin.getLogger().severe("✗ Failed to register command: " + error.getMessage())
                        );

                plugin.getLogger().info("Discord bot is fully operational!");

            } catch (InterruptedException e) {
                plugin.getLogger().severe("JDA initialization was interrupted!");
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                plugin.getLogger().severe("Failed to initialize JDA: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    public void shutdown() {
        if (jda != null) {
            try {
                plugin.getLogger().info("Shutting down JDA...");
                jda.getRegisteredListeners().forEach(jda::removeEventListener);
                jda.shutdownNow();
                plugin.getLogger().info("JDA shutdown initiated");
            } catch (Exception e) {
                plugin.getLogger().info("JDA shutdown completed with minor warnings (normal for hot reload)");
            }
        }
    }

    public JDA getJda() {
        return jda;
    }
}