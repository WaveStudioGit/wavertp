package com.wavestudio.rtp.dialog;

import com.wavestudio.rtp.RtpPlugin;
import com.wavestudio.rtp.config.MessageProvider;
import com.wavestudio.rtp.config.RtpConfig;
import com.wavestudio.rtp.model.Dimension;
import com.wavestudio.rtp.rtp.SafeLocationFinder;
import com.wavestudio.rtp.util.CooldownManager;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogBase;
import io.papermc.paper.dialog.DialogType;
import io.papermc.paper.registry.data.dialog.DialogBody;
import io.papermc.paper.registry.data.dialog.DialogInput;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;

public class RtpDialogFactory {
    private final RtpPlugin plugin;
    private final RtpConfig config;
    private final MessageProvider messages;
    private final SafeLocationFinder locationFinder;
    private final CooldownManager cooldownManager;

    public RtpDialogFactory(RtpPlugin plugin, RtpConfig config, MessageProvider messages,
                            SafeLocationFinder locationFinder, CooldownManager cooldownManager) {
        this.plugin = plugin;
        this.config = config;
        this.messages = messages;
        this.locationFinder = locationFinder;
        this.cooldownManager = cooldownManager;
    }

    public void showRtpDialog(Player player) {
        if (cooldownManager.isOnCooldown(player.getUniqueId())) {
            long remaining = cooldownManager.getRemainingSeconds(player.getUniqueId());
            player.sendMessage(messages.parse("command.cooldown", Map.of("seconds", String.valueOf(remaining))));
            return;
        }

        Dialog dialog = createMainDialog();
        player.showDialog(dialog);
    }

    private Dialog createMainDialog() {
        List<DialogBody> body = createBody();
        List<DialogInput> inputs = createInputs();

        DialogBase base = DialogBase.builder(messages.parse("dialog.title"))
                .body(body)
                .inputs(inputs)
                .canCloseWithEscape(true)
                .build();

        return Dialog.create(builder -> builder.empty()
                .base(base)
                .type(DialogType.confirmation(
                        createConfirmButton(),
                        createCancelButton()
                )));
    }

    private List<DialogBody> createBody() {
        List<DialogBody> body = new ArrayList<>();
        
        body.add(DialogBody.plainMessage(messages.parse("dialog.body")));
        body.add(DialogBody.plainMessage(Component.space()));
        
        for (Dimension dim : Dimension.values()) {
            ItemStack item = createDimensionItem(dim);
            Component description = messages.dimensionItemDescription(dim);
            
            body.add(DialogBody.item(
                    item,
                    description,
                    true,  // showDecorations
                    true,  // showTooltip
                    80,    // width
                    80     // height
            ));
        }
        
        return body;
    }

    private ItemStack createDimensionItem(Dimension dimension) {
        return switch (dimension) {
            case OVERWORLD -> new ItemStack(Material.GRASS_BLOCK);
            case NETHER -> new ItemStack(Material.NETHERRACK);
            case END -> new ItemStack(Material.END_STONE);
        };
    }

    private List<DialogInput> createInputs() {
        List<SingleOptionDialogInput.OptionEntry> options = new ArrayList<>();
        
        for (Dimension dim : Dimension.values()) {
            Component display = messages.dimensionName(dim);
            boolean initial = (dim == Dimension.OVERWORLD);
            options.add(SingleOptionDialogInput.OptionEntry.create(dim.getId(), display, initial));
        }

        SingleOptionDialogInput.Builder inputBuilder = DialogInput.singleOption(
                "dimension",
                messages.parse("dialog.dimension-label"),
                options
        );

        return List.of(inputBuilder.build());
    }

    private io.papermc.paper.registry.data.dialog.ActionButton createConfirmButton() {
        return io.papermc.paper.registry.data.dialog.ActionButton.builder(
                messages.parse("dialog.confirm-button"))
                .tooltip(messages.parse("dialog.confirm-tooltip"))
                .action(DialogAction.customClick((response, audience) -> {
                    handleConfirm(response, audience);
                }, ClickCallback.Options.builder().build()))
                .build();
    }

    private io.papermc.paper.registry.data.dialog.ActionButton createCancelButton() {
        return io.papermc.paper.registry.data.dialog.ActionButton.builder(
                messages.parse("dialog.cancel-button"))
                .tooltip(messages.parse("dialog.cancel-tooltip"))
                .action(DialogAction.customClick((response, audience) -> {
                    if (audience instanceof Player player) {
                        player.sendMessage(messages.prefixed("dialog.cancelled", Map.of()));
                    }
                }, ClickCallback.Options.builder().build()))
                .build();
    }

    private void handleConfirm(io.papermc.paper.dialog.DialogResponseView response, Audience audience) {
        if (!(audience instanceof Player player)) return;

        String dimensionId = response.getText("dimension");
        if (dimensionId == null) {
            player.sendMessage(messages.prefixed("error.internal-error", Map.of()));
            return;
        }

        Dimension dimension = Dimension.fromId(dimensionId);
        if (dimension == null) {
            player.sendMessage(messages.prefixed("error.internal-error", Map.of()));
            return;
        }

        World world = Bukkit.getWorld(config.getWorldName(dimension));
        if (world == null) {
            player.sendMessage(messages.prefixed("error.world-not-found", Map.of("world", config.getWorldName(dimension))));
            return;
        }

        cooldownManager.setCooldown(player.getUniqueId());

        player.sendMessage(messages.parse("process.searching", Map.of(
                "dimension", dimension.getName(),
                "attempt", "1",
                "max", String.valueOf(config.getMaxAttempts())
        )));

        locationFinder.findSafeLocation(player, dimension)
                .thenAccept(optionalLoc -> {
                    if (!player.isOnline()) return;
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        if (optionalLoc.isPresent()) {
                            performTeleport(player, dimension, optionalLoc.get());
                        } else {
                            player.sendMessage(messages.prefixed("process.failed", Map.of(
                                    "attempts", String.valueOf(config.getMaxAttempts())
                            )));
                            cooldownManager.removeCooldown(player.getUniqueId());
                        }
                    });
                });
    }

    private void performTeleport(Player player, Dimension dimension, Vector location) {
        World world = Bukkit.getWorld(config.getWorldName(dimension));
        if (world == null) {
            player.sendMessage(messages.prefixed("error.world-not-found", Map.of("world", config.getWorldName(dimension))));
            return;
        }

        org.bukkit.Location bukkitLoc = new org.bukkit.Location(world, location.getX(), location.getY(), location.getZ());
        
        playTeleportEffects(player, bukkitLoc);
        
        player.teleport(bukkitLoc);
        
        player.sendMessage(messages.prefixed("result.success", Map.of(
                "dimension", dimension.getName()
        )));
    }

    private void playTeleportEffects(Player player, org.bukkit.Location destination) {
        Map<String, Object> effects = config.getEffectsSettings();
        
        String soundName = (String) effects.getOrDefault("sound", "ENTITY_ENDERMAN_TELEPORT");
        String sourceParticle = (String) effects.getOrDefault("source-particle", "PORTAL");
        String destParticle = (String) effects.getOrDefault("dest-particle", "END_ROD");
        int particleCount = (int) effects.getOrDefault("particle-count", 30);

        try {
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
            player.getWorld().playSound(player.getLocation(), sound, 1.0f, 1.0f);
            destination.getWorld().playSound(destination, sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {}

        try {
            org.bukkit.Particle particle = org.bukkit.Particle.valueOf(sourceParticle);
            player.getWorld().spawnParticle(particle, player.getLocation(), particleCount, 0.5, 0.5, 0.5, 0.1);
        } catch (IllegalArgumentException ignored) {}

        try {
            org.bukkit.Particle particle = org.bukkit.Particle.valueOf(destParticle);
            destination.getWorld().spawnParticle(particle, destination, particleCount, 0.5, 1.0, 0.5, 0.1);
        } catch (IllegalArgumentException ignored) {}
    }
}