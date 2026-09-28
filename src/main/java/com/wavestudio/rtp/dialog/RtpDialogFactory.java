package com.wavestudio.rtp.dialog;

import com.wavestudio.rtp.RtpPlugin;
import com.wavestudio.rtp.config.MessageProvider;
import com.wavestudio.rtp.config.RtpConfig;
import com.wavestudio.rtp.model.Dimension;
import com.wavestudio.rtp.rtp.SafeLocationFinder;
import com.wavestudio.rtp.util.CooldownManager;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        if (cooldownManager.isOnCooldown(player)) {
            long remaining = cooldownManager.getRemainingSeconds(player);
            player.sendMessage(messages.parse("command.cooldown", Map.of("seconds", String.valueOf(remaining))));
            return;
        }

        Dialog dialog = createMainDialog();
        player.showDialog(dialog);
    }

    public void startTeleport(Player player, Dimension dimension) {
        World world = Bukkit.getWorld(config.getWorldName(dimension));
        if (world == null) {
            player.sendMessage(messages.prefixed("error.world-not-found", Map.of("world", config.getWorldName(dimension))));
            return;
        }

        if (cooldownManager.isOnCooldown(player)) {
            long remaining = cooldownManager.getRemainingSeconds(player);
            player.sendMessage(messages.parse("command.cooldown", Map.of("seconds", String.valueOf(remaining))));
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

                    player.getScheduler().run(plugin, task -> {
                        if (optionalLoc.isPresent()) {
                            performTeleport(player, dimension, optionalLoc.get());
                        } else {
                            player.sendMessage(messages.prefixed("process.failed", Map.of(
                                    "attempts", String.valueOf(config.getMaxAttempts())
                            )));
                            cooldownManager.removeCooldown(player.getUniqueId());
                        }
                    }, null);
                });
    }

    private Dialog createMainDialog() {
        List<DialogBody> body = createBody();
        List<DialogInput> inputs = createInputs();

        DialogBase base = DialogBase.builder(messages.parse("dialog.title"))
                .body(body)
                .inputs(inputs)
                .canCloseWithEscape(true)
                .build();

        ActionButton confirm = createConfirmButton();
        ActionButton cancel = createCancelButton();

        return Dialog.create(b -> b.empty().base(base).type(DialogType.confirmation(confirm, cancel)));
    }

    private List<DialogBody> createBody() {
        List<DialogBody> body = new ArrayList<>();

        body.add(DialogBody.plainMessage(messages.parse("dialog.body")));

        for (Dimension dim : Dimension.values()) {
            ItemStack item = createDimensionItem(dim);
            Component description = messages.dimensionItemDescription(dim);

            body.add(DialogBody.item(
                    item,
                    DialogBody.plainMessage(description),
                    true,
                    true,
                    40,
                    40
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

    private ActionButton createConfirmButton() {
        return ActionButton.builder(messages.parse("dialog.confirm-button"))
                .tooltip(messages.parse("dialog.confirm-tooltip"))
                .action(DialogAction.customClick(
                        (response, audience) -> handleConfirm(response, audience),
                        ClickCallback.Options.builder().build()
                ))
                .build();
    }

    private ActionButton createCancelButton() {
        return ActionButton.builder(messages.parse("dialog.cancel-button"))
                .tooltip(messages.parse("dialog.cancel-tooltip"))
                .action(DialogAction.customClick(
                        (response, audience) -> {
                            if (audience instanceof Player player) {
                                player.sendMessage(messages.prefixed("dialog.cancelled", Map.of()));
                            }
                        },
                        ClickCallback.Options.builder().build()
                ))
                .build();
    }

    private void handleConfirm(DialogResponseView response, Audience audience) {
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

        startTeleport(player, dimension);
    }

    private void performTeleport(Player player, Dimension dimension, Vector location) {
        World world = Bukkit.getWorld(config.getWorldName(dimension));
        if (world == null) {
            player.sendMessage(messages.prefixed("error.world-not-found", Map.of("world", config.getWorldName(dimension))));
            cooldownManager.removeCooldown(player.getUniqueId());
            return;
        }

        org.bukkit.Location bukkitLoc = new org.bukkit.Location(world, location.getX(), location.getY(), location.getZ());

        playSourceEffects(player);

        player.teleportAsync(bukkitLoc).thenAccept(ok -> {
            if (Boolean.TRUE.equals(ok)) {
                try {
                    Bukkit.getRegionScheduler().run(plugin, world,
                            bukkitLoc.getBlockX() >> 4, bukkitLoc.getBlockZ() >> 4,
                            task -> {
                                playDestEffects(bukkitLoc);
                                player.sendMessage(messages.prefixed("result.success", Map.of(
                                        "dimension", dimension.getName()
                                )));
                            });
                } catch (Exception e) {
                    playDestEffects(bukkitLoc);
                    player.sendMessage(messages.prefixed("result.success", Map.of(
                            "dimension", dimension.getName()
                    )));
                }
            } else {
                player.sendMessage(messages.prefixed("result.failed-generic", Map.of()));
                cooldownManager.removeCooldown(player.getUniqueId());
            }
        });
    }

    private void playSourceEffects(Player player) {
        Map<String, Object> effects = config.getEffectsSettings();

        String soundName = (String) effects.getOrDefault("sound", "ENTITY_ENDERMAN_TELEPORT");
        String sourceParticle = (String) effects.getOrDefault("source-particle", "PORTAL");
        int particleCount = (int) effects.getOrDefault("particle-count", 30);

        try {
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
            player.getWorld().playSound(player.getLocation(), sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {}

        try {
            org.bukkit.Particle particle = org.bukkit.Particle.valueOf(sourceParticle);
            player.getWorld().spawnParticle(particle, player.getLocation(), particleCount, 0.5, 0.5, 0.5, 0.1);
        } catch (IllegalArgumentException ignored) {}
    }

    private void playDestEffects(org.bukkit.Location destination) {
        Map<String, Object> effects = config.getEffectsSettings();

        String soundName = (String) effects.getOrDefault("sound", "ENTITY_ENDERMAN_TELEPORT");
        String destParticle = (String) effects.getOrDefault("dest-particle", "END_ROD");
        int particleCount = (int) effects.getOrDefault("particle-count", 30);

        try {
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
            destination.getWorld().playSound(destination, sound, 1.0f, 1.0f);
        } catch (IllegalArgumentException ignored) {}

        try {
            org.bukkit.Particle particle = org.bukkit.Particle.valueOf(destParticle);
            destination.getWorld().spawnParticle(particle, destination, particleCount, 0.5, 1.0, 0.5, 0.1);
        } catch (IllegalArgumentException ignored) {}
    }
}
