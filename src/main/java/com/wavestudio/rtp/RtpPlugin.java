package com.wavestudio.rtp;

import com.wavestudio.rtp.command.RtpCommand;
import com.wavestudio.rtp.config.MessageProvider;
import com.wavestudio.rtp.config.RtpConfig;
import com.wavestudio.rtp.dialog.RtpDialogFactory;
import com.wavestudio.rtp.rtp.SafeLocationFinder;
import com.wavestudio.rtp.util.CooldownManager;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

public class RtpPlugin extends JavaPlugin implements PluginBootstrap {
    private RtpConfig config;
    private MessageProvider messageProvider;
    private SafeLocationFinder locationFinder;
    private CooldownManager cooldownManager;
    private RtpDialogFactory dialogFactory;
    private RtpCommand rtpCommand;

    @Override
    public void bootstrap(BootstrapContext context) {
        getLogger().info("Bootstrap called - registering commands");
        context.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            getLogger().info("COMMANDS lifecycle event triggered");
            var registrar = event.registrar();
            
            // Initialize dependencies
            this.config = new RtpConfig(this);
            this.messageProvider = new MessageProvider(config);
            this.cooldownManager = new CooldownManager(config.getCooldownSeconds());
            this.locationFinder = new SafeLocationFinder(this, config);
            this.dialogFactory = new RtpDialogFactory(this, config, messageProvider, locationFinder, cooldownManager);

            this.rtpCommand = new RtpCommand(this, config, messageProvider, dialogFactory, locationFinder, cooldownManager);

            var command = Commands.literal("rtp")
                .executes(ctx -> {
                    if (!(ctx.getSource() instanceof CommandSourceStack)) return 0;
                    var sender = ctx.getSource().getSender();
                    if (!(sender instanceof org.bukkit.entity.Player player)) {
                        sender.sendMessage(messageProvider.parse("command.player-only"));
                        return 1;
                    }
                    dialogFactory.showRtpDialog(player);
                    return 1;
                })
                .then(Commands.literal("reload")
                    .requires(source -> source.getSender().hasPermission("wavertp.admin"))
                    .executes(ctx -> {
                        reloadConfig();
                        config.reload();
                        cooldownManager.clear();
                        ctx.getSource().getSender().sendMessage(messageProvider.parse("command.reloaded"));
                        return 1;
                    })
                )
                .then(Commands.literal("cooldown")
                    .requires(source -> source.getSender().hasPermission("wavertp.admin"))
                    .then(Commands.literal("set")
                        .executes(ctx -> {
                            var sender = ctx.getSource().getSender();
                            if (sender instanceof org.bukkit.entity.Player player) {
                                cooldownManager.setCooldown(player.getUniqueId());
                                sender.sendMessage(messageProvider.parseRaw("<green>Cooldown set for " + player.getName()));
                            }
                            return 1;
                        })
                    )
                    .then(Commands.literal("clear")
                        .executes(ctx -> {
                            var sender = ctx.getSource().getSender();
                            if (sender instanceof org.bukkit.entity.Player player) {
                                cooldownManager.removeCooldown(player.getUniqueId());
                                sender.sendMessage(messageProvider.parseRaw("<green>Cooldown cleared for " + player.getName()));
                            }
                            return 1;
                        })
                    )
                    .then(Commands.literal("check")
                        .executes(ctx -> {
                            var sender = ctx.getSource().getSender();
                            if (sender instanceof org.bukkit.entity.Player player) {
                                long remaining = cooldownManager.getRemainingSeconds(player.getUniqueId());
                                if (remaining > 0) {
                                    sender.sendMessage(messageProvider.parseRaw("<yellow>" + player.getName() + " has " + remaining + "s cooldown remaining."));
                                } else {
                                    sender.sendMessage(messageProvider.parseRaw("<green>" + player.getName() + " has no cooldown."));
                                }
                            }
                            return 1;
                        })
                    )
                )
                .build();
            
            getLogger().info("Registering rtp command...");
            registrar.register(command);
            getLogger().info("rtp command registered successfully");
        });
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("config.yml", false);

        getLogger().info("MultiDimensionRTP enabled successfully!");
        getLogger().info("By wavestudio");
    }

    @Override
    public void onDisable() {
        if (locationFinder != null) {
            locationFinder.shutdown();
        }
        if (cooldownManager != null) {
            cooldownManager.clear();
        }
        getLogger().info("MultiDimensionRTP disabled.");
    }

    public RtpConfig getRtpConfig() {
        return config;
    }

    public MessageProvider getMessageProvider() {
        return messageProvider;
    }
}