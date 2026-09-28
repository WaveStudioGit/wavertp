package com.wavestudio.rtp;

import com.wavestudio.rtp.command.RtpCommand;
import com.wavestudio.rtp.config.MessageProvider;
import com.wavestudio.rtp.config.RtpConfig;
import com.wavestudio.rtp.dialog.RtpDialogFactory;
import com.wavestudio.rtp.model.Dimension;
import com.wavestudio.rtp.rtp.SafeLocationFinder;
import com.wavestudio.rtp.util.CooldownManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Map;

public class RtpPlugin extends JavaPlugin {
    private RtpConfig config;
    private MessageProvider messageProvider;
    private SafeLocationFinder locationFinder;
    private CooldownManager cooldownManager;
    private RtpDialogFactory dialogFactory;
    private RtpCommand rtpCommand;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        saveResource("config.yml", false);

        this.config = new RtpConfig(this);
        this.messageProvider = new MessageProvider(config);
        this.cooldownManager = new CooldownManager(config.getCooldownSeconds());
        this.locationFinder = new SafeLocationFinder(this, config);
        this.dialogFactory = new RtpDialogFactory(this, config, messageProvider, locationFinder, cooldownManager);
        this.rtpCommand = new RtpCommand(this, config, messageProvider, dialogFactory, locationFinder, cooldownManager);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            getLogger().info("Registering /rtp via Brigadier");
            event.registrar().register(
                    Commands.literal("rtp")
                            .executes(ctx -> {
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
                                    }))
                            .then(Commands.argument("dimension", StringArgumentType.word())
                                    .suggests((ctx, builder) -> {
                                        for (Dimension dim : Dimension.values()) {
                                            builder.suggest(dim.getId());
                                        }
                                        return builder.buildFuture();
                                    })
                                    .executes(ctx -> {
                                        var sender = ctx.getSource().getSender();
                                        if (!(sender instanceof org.bukkit.entity.Player player)) {
                                            sender.sendMessage(messageProvider.parse("command.player-only"));
                                            return 1;
                                        }
                                        String raw = ctx.getArgument("dimension", String.class);
                                        Dimension dim = Dimension.fromId(raw);
                                        if (dim == null) {
                                            sender.sendMessage(messageProvider.prefixed("error.internal-error", Map.of()));
                                            return 0;
                                        }
                                        dialogFactory.startTeleport(player, dim);
                                        return 1;
                                    }))
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
                                            }))
                                    .then(Commands.literal("clear")
                                            .executes(ctx -> {
                                                var sender = ctx.getSource().getSender();
                                                if (sender instanceof org.bukkit.entity.Player player) {
                                                    cooldownManager.removeCooldown(player.getUniqueId());
                                                    sender.sendMessage(messageProvider.parseRaw("<green>Cooldown cleared for " + player.getName()));
                                                }
                                                return 1;
                                            }))
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
                                            }))
                            )
                            .build(),
                    "Random teleport",
                    List.of()
            );
            getLogger().info("/rtp registered via Brigadier");
        });

        getLogger().info("wavertp enabled");
    }

    @Override
    public void onDisable() {
        if (locationFinder != null) {
            locationFinder.shutdown();
        }
        if (cooldownManager != null) {
            cooldownManager.clear();
        }
        getLogger().info("wavertp disabled");
    }

    public RtpConfig getRtpConfig() {
        return config;
    }

    public MessageProvider getMessageProvider() {
        return messageProvider;
    }
}
