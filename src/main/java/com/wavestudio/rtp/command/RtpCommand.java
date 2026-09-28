package com.wavestudio.rtp.command;

import com.wavestudio.rtp.RtpPlugin;
import com.wavestudio.rtp.config.MessageProvider;
import com.wavestudio.rtp.config.RtpConfig;
import com.wavestudio.rtp.dialog.RtpDialogFactory;
import com.wavestudio.rtp.rtp.SafeLocationFinder;
import com.wavestudio.rtp.util.CooldownManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

public class RtpCommand implements CommandExecutor, TabCompleter {
    private final RtpPlugin plugin;
    private final RtpConfig config;
    private final MessageProvider messages;
    private final RtpDialogFactory dialogFactory;
    private final SafeLocationFinder locationFinder;
    private final CooldownManager cooldownManager;

    public RtpCommand(RtpPlugin plugin, RtpConfig config, MessageProvider messages,
                      RtpDialogFactory dialogFactory, SafeLocationFinder locationFinder,
                      CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.dialogFactory = dialogFactory;
        this.locationFinder = locationFinder;
        this.cooldownManager = cooldownManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(messages.parse("command.player-only"));
            return true;
        }

        if (args.length > 0) {
            String subCommand = args[0].toLowerCase();
            switch (subCommand) {
                case "reload" -> {
                    if (!player.hasPermission("rtp.admin")) {
                        player.sendMessage(messages.parse("command.no-permission"));
                        return true;
                    }
                    reloadConfig(player);
                    return true;
                }
                case "cooldown" -> {
                    if (!player.hasPermission("rtp.admin")) {
                        player.sendMessage(messages.parse("command.no-permission"));
                        return true;
                    }
                    handleCooldownCommand(player, args);
                    return true;
                }
                default -> {
                    player.sendMessage(messages.parse("command.no-permission"));
                    return true;
                }
            }
        }

        dialogFactory.showRtpDialog(player);
        return true;
    }

    private void reloadConfig(Player player) {
        plugin.reloadConfig();
        config.reload();
        cooldownManager.clear();
        player.sendMessage(messages.parse("command.reloaded"));
    }

    private void handleCooldownCommand(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(messages.parseRaw("<red>Usage: /rtp cooldown <set|clear|check> [player]"));
            return;
        }

        String action = args[1].toLowerCase();
        Player target = args.length > 2 ? plugin.getServer().getPlayer(args[2]) : player;
        
        if (target == null) {
            player.sendMessage(messages.parseRaw("<red>Player not found."));
            return;
        }

        switch (action) {
            case "set" -> {
                cooldownManager.setCooldown(target.getUniqueId());
                player.sendMessage(messages.parseRaw("<green>Cooldown set for " + target.getName()));
            }
            case "clear" -> {
                cooldownManager.removeCooldown(target.getUniqueId());
                player.sendMessage(messages.parseRaw("<green>Cooldown cleared for " + target.getName()));
            }
            case "check" -> {
                long remaining = cooldownManager.getRemainingSeconds(target.getUniqueId());
                if (remaining > 0) {
                    player.sendMessage(messages.parseRaw("<yellow>" + target.getName() + " has " + remaining + "s cooldown remaining."));
                } else {
                    player.sendMessage(messages.parseRaw("<green>" + target.getName() + " has no cooldown."));
                }
            }
            default -> player.sendMessage(messages.parseRaw("<red>Unknown action: " + action));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return List.of("reload", "cooldown");
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("cooldown")) {
            return List.of("set", "clear", "check");
        }
        return Collections.emptyList();
    }
}