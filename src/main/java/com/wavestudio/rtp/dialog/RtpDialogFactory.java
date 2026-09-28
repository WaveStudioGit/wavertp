package com.wavestudio.rtp.dialog;

import com.wavestudio.rtp.RtpPlugin;
import com.wavestudio.rtp.config.MessageProvider;
import com.wavestudio.rtp.config.RtpConfig;
import com.wavestudio.rtp.model.Dimension;
import com.wavestudio.rtp.rtp.SafeLocationFinder;
import com.wavestudio.rtp.util.CooldownManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RtpDialogFactory {
    private final RtpPlugin plugin;
    private final RtpConfig config;
    private final MessageProvider messages;
    private final SafeLocationFinder locationFinder;
    private final CooldownManager cooldownManager;

    // Reflection handles for Dialog API
    private static final Class<?> DIALOG_CLASS;
    private static final Class<?> DIALOG_BASE_CLASS;
    private static final Class<?> DIALOG_TYPE_CLASS;
    private static final Class<?> DIALOG_BODY_CLASS;
    private static final Class<?> DIALOG_INPUT_CLASS;
    private static final Class<?> DIALOG_ACTION_CLASS;
    private static final Class<?> ACTION_BUTTON_CLASS;
    private static final Class<?> SINGLE_OPTION_INPUT_CLASS;
    private static final Class<?> OPTION_ENTRY_CLASS;
    private static final Class<?> DIALOG_RESPONSE_VIEW_CLASS;
    private static final Class<?> CLICK_CALLBACK_OPTIONS_CLASS;

    private static final MethodHandle DIALOG_CREATE;
    private static final MethodHandle DIALOG_BASE_BUILDER;
    private static final MethodHandle DIALOG_BASE_BUILD;
    private static final MethodHandle DIALOG_BODY_PLAIN_MESSAGE;
    private static final MethodHandle DIALOG_BODY_ITEM;
    private static final MethodHandle DIALOG_INPUT_SINGLE_OPTION;
    private static final MethodHandle SINGLE_OPTION_BUILD;
    private static final MethodHandle OPTION_ENTRY_CREATE;
    private static final MethodHandle ACTION_BUTTON_BUILDER;
    private static final MethodHandle ACTION_BUTTON_TOOLTIP;
    private static final MethodHandle ACTION_BUTTON_ACTION;
    private static final MethodHandle ACTION_BUTTON_BUILD;
    private static final MethodHandle DIALOG_TYPE_CONFIRMATION;
    private static final MethodHandle DIALOG_ACTION_CUSTOM_CLICK;
    private static final MethodHandle CLICK_CALLBACK_OPTIONS_BUILDER;
    private static final MethodHandle CLICK_CALLBACK_OPTIONS_BUILD;
    private static final MethodHandle RESPONSE_GET_TEXT;

    static {
        Class<?> dialogClass = null;
        Class<?> dialogBaseClass = null;
        Class<?> dialogTypeClass = null;
        Class<?> dialogBodyClass = null;
        Class<?> dialogInputClass = null;
        Class<?> dialogActionClass = null;
        Class<?> actionButtonClass = null;
        Class<?> singleOptionInputClass = null;
        Class<?> optionEntryClass = null;
        Class<?> dialogResponseViewClass = null;
        Class<?> clickCallbackOptionsClass = null;

        MethodHandle dc = null, dbb = null, db = null, dbpm = null, dbi = null;
        MethodHandle dib = null, soib = null, oec = null, abb = null, abt = null, aba = null, abu = null;
        MethodHandle dtc = null, dac = null, ccob = null, ccobu = null, rgt = null;

        try {
            dialogClass = Class.forName("io.papermc.paper.dialog.Dialog");
            dialogBaseClass = Class.forName("io.papermc.paper.dialog.DialogBase");
            dialogTypeClass = Class.forName("io.papermc.paper.dialog.DialogType");
            dialogBodyClass = Class.forName("io.papermc.paper.registry.data.dialog.DialogBody");
            dialogInputClass = Class.forName("io.papermc.paper.registry.data.dialog.DialogInput");
            dialogActionClass = Class.forName("io.papermc.paper.registry.data.dialog.action.DialogAction");
            actionButtonClass = Class.forName("io.papermc.paper.registry.data.dialog.ActionButton");
            singleOptionInputClass = Class.forName("io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput");
            optionEntryClass = Class.forName("io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput$OptionEntry");
            dialogResponseViewClass = Class.forName("io.papermc.paper.dialog.DialogResponseView");
            clickCallbackOptionsClass = Class.forName("net.kyori.adventure.text.event.ClickCallback$Options");

            MethodHandles.Lookup lookup = MethodHandles.lookup();

            // Dialog.create(Consumer<Dialog.Builder>)
            Method createMethod = dialogClass.getMethod("create", java.util.function.Consumer.class);
            dc = lookup.unreflect(createMethod);

            // DialogBase.builder(Component)
            Method baseBuilderMethod = dialogBaseClass.getMethod("builder", Component.class);
            dbb = lookup.unreflect(baseBuilderMethod);

            // DialogBase.Builder.build()
            Class<?> baseBuilderClass = Class.forName("io.papermc.paper.dialog.DialogBase$Builder");
            Method baseBuildMethod = baseBuilderClass.getMethod("build");
            db = lookup.unreflect(baseBuildMethod);

            // DialogBody.plainMessage(Component)
            Method plainMessageMethod = dialogBodyClass.getMethod("plainMessage", Component.class);
            dbpm = lookup.unreflect(plainMessageMethod);

            // DialogBody.item(ItemStack, Component, boolean, boolean, int, int)
            Method itemMethod = dialogBodyClass.getMethod("item", ItemStack.class, Component.class, boolean.class, boolean.class, int.class, int.class);
            dbi = lookup.unreflect(itemMethod);

            // DialogInput.singleOption(String, Component, List)
            Method singleOptionMethod = dialogInputClass.getMethod("singleOption", String.class, Component.class, List.class);
            dib = lookup.unreflect(singleOptionMethod);

            // SingleOptionDialogInput.Builder.build()
            Class<?> singleOptionBuilderClass = Class.forName("io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput$Builder");
            Method soBuildMethod = singleOptionBuilderClass.getMethod("build");
            soib = lookup.unreflect(soBuildMethod);

            // OptionEntry.create(String, Component, boolean)
            Method optionEntryMethod = optionEntryClass.getMethod("create", String.class, Component.class, boolean.class);
            oec = lookup.unreflect(optionEntryMethod);

            // ActionButton.builder(Component)
            Method abBuilderMethod = actionButtonClass.getMethod("builder", Component.class);
            abb = lookup.unreflect(abBuilderMethod);

            // ActionButton.Builder.tooltip(Component)
            Class<?> abBuilderClass = Class.forName("io.papermc.paper.registry.data.dialog.ActionButton$Builder");
            Method tooltipMethod = abBuilderClass.getMethod("tooltip", Component.class);
            abt = lookup.unreflect(tooltipMethod);

            // ActionButton.Builder.action(DialogAction)
            Method actionMethod = abBuilderClass.getMethod("action", dialogActionClass);
            aba = lookup.unreflect(actionMethod);

            // ActionButton.Builder.build()
            Method abBuildMethod = abBuilderClass.getMethod("build");
            abu = lookup.unreflect(abBuildMethod);

            // DialogType.confirmation(ActionButton, ActionButton)
            Method confirmationMethod = dialogTypeClass.getMethod("confirmation", actionButtonClass, actionButtonClass);
            dtc = lookup.unreflect(confirmationMethod);

            // DialogAction.customClick(DialogActionCallback, ClickCallback.Options)
            Method customClickMethod = dialogActionClass.getMethod("customClick", Class.forName("io.papermc.paper.registry.data.dialog.action.DialogActionCallback"), clickCallbackOptionsClass);
            dac = lookup.unreflect(customClickMethod);

            // ClickCallback.Options.builder()
            Method ccobMethod = clickCallbackOptionsClass.getMethod("builder");
            ccob = lookup.unreflect(ccobMethod);

            // ClickCallback.Options.Builder.build()
            Class<?> ccobBuilderClass = Class.forName("net.kyori.adventure.text.event.ClickCallback$Options$Builder");
            Method ccobuMethod = ccobBuilderClass.getMethod("build");
            ccobu = lookup.unreflect(ccobuMethod);

            // DialogResponseView.getText(String)
            Method getTextMethod = dialogResponseViewClass.getMethod("getText", String.class);
            rgt = lookup.unreflect(getTextMethod);

        } catch (ReflectiveOperationException e) {
            // Dialog API not available - will use fallback
        }

        DIALOG_CLASS = dialogClass;
        DIALOG_BASE_CLASS = dialogBaseClass;
        DIALOG_TYPE_CLASS = dialogTypeClass;
        DIALOG_BODY_CLASS = dialogBodyClass;
        DIALOG_INPUT_CLASS = dialogInputClass;
        DIALOG_ACTION_CLASS = dialogActionClass;
        ACTION_BUTTON_CLASS = actionButtonClass;
        SINGLE_OPTION_INPUT_CLASS = singleOptionInputClass;
        OPTION_ENTRY_CLASS = optionEntryClass;
        DIALOG_RESPONSE_VIEW_CLASS = dialogResponseViewClass;
        CLICK_CALLBACK_OPTIONS_CLASS = clickCallbackOptionsClass;

        DIALOG_CREATE = dc;
        DIALOG_BASE_BUILDER = dbb;
        DIALOG_BASE_BUILD = db;
        DIALOG_BODY_PLAIN_MESSAGE = dbpm;
        DIALOG_BODY_ITEM = dbi;
        DIALOG_INPUT_SINGLE_OPTION = dib;
        SINGLE_OPTION_BUILD = soib;
        OPTION_ENTRY_CREATE = oec;
        ACTION_BUTTON_BUILDER = abb;
        ACTION_BUTTON_TOOLTIP = abt;
        ACTION_BUTTON_ACTION = aba;
        ACTION_BUTTON_BUILD = abu;
        DIALOG_TYPE_CONFIRMATION = dtc;
        DIALOG_ACTION_CUSTOM_CLICK = dac;
        CLICK_CALLBACK_OPTIONS_BUILDER = ccob;
        CLICK_CALLBACK_OPTIONS_BUILD = ccobu;
        RESPONSE_GET_TEXT = rgt;
    }

    private static boolean isDialogApiAvailable() {
        return DIALOG_CLASS != null;
    }

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

        if (isDialogApiAvailable()) {
            try {
                Object dialog = createMainDialog();
                Method showDialog = player.getClass().getMethod("showDialog", DIALOG_CLASS);
                showDialog.invoke(player, dialog);
            } catch (ReflectiveOperationException e) {
                plugin.getLogger().warning("Failed to show dialog, using fallback: " + e.getMessage());
                showFallbackMenu(player);
            }
        } else {
            showFallbackMenu(player);
        }
    }

    private Object createMainDialog() throws ReflectiveOperationException {
        List<Object> body = createBody();
        List<Object> inputs = createInputs();

        Object baseBuilder = invoke(DIALOG_BASE_BUILDER, null, messages.parse("dialog.title"));
        Object base = buildDialogBase(baseBuilder, body, inputs);
        
        Object confirmButton = createConfirmButton();
        Object cancelButton = createCancelButton();
        Object dialogType = invoke(DIALOG_TYPE_CONFIRMATION, null, confirmButton, cancelButton);

        Object builder = invokeEmptyDialogBuilder();
        setDialogBuilderBase(builder, base);
        setDialogBuilderType(builder, dialogType);

        return invoke(DIALOG_CREATE, null, (java.util.function.Consumer<Object>) b -> {});
    }

    private Object buildDialogBase(Object baseBuilder, List<Object> body, List<Object> inputs) throws ReflectiveOperationException {
        invoke(void.class, baseBuilder.getClass().getMethod("body", List.class), baseBuilder, body);
        invoke(void.class, baseBuilder.getClass().getMethod("inputs", List.class), baseBuilder, inputs);
        invoke(void.class, baseBuilder.getClass().getMethod("canCloseWithEscape", boolean.class), baseBuilder, true);
        return invoke(baseBuilder.getClass().getMethod("build"), baseBuilder);
    }

    private Object invokeEmptyDialogBuilder() throws ReflectiveOperationException {
        return invoke(DIALOG_CLASS.getMethod("empty"), null);
    }

    private void setDialogBuilderBase(Object builder, Object base) throws ReflectiveOperationException {
        invoke(void.class, builder.getClass().getMethod("base", DIALOG_BASE_CLASS), builder, base);
    }

    private void setDialogBuilderType(Object builder, Object type) throws ReflectiveOperationException {
        invoke(void.class, builder.getClass().getMethod("type", DIALOG_TYPE_CLASS), builder, type);
    }

    private List<Object> createBody() throws ReflectiveOperationException {
        List<Object> body = new ArrayList<>();

        body.add(invoke(DIALOG_BODY_PLAIN_MESSAGE, null, messages.parse("dialog.body")));
        body.add(invoke(DIALOG_BODY_PLAIN_MESSAGE, null, Component.space()));

        for (Dimension dim : Dimension.values()) {
            ItemStack item = createDimensionItem(dim);
            Component description = messages.dimensionItemDescription(dim);
            Object itemBody = invoke(DIALOG_BODY_ITEM, null, item, description, true, true, 80, 80);
            body.add(itemBody);
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

    private List<Object> createInputs() throws ReflectiveOperationException {
        List<Object> options = new ArrayList<>();

        for (Dimension dim : Dimension.values()) {
            Component display = messages.dimensionName(dim);
            boolean initial = (dim == Dimension.OVERWORLD);
            Object option = invoke(OPTION_ENTRY_CREATE, null, dim.getId(), messages.dimensionName(dim), initial);
            options.add(option);
        }

        Object inputBuilder = invoke(DIALOG_INPUT_SINGLE_OPTION, null, "dimension", messages.parse("dialog.dimension-label"), options);
        Object input = invoke(SINGLE_OPTION_BUILD, inputBuilder);

        return List.of(input);
    }

    private Object createConfirmButton() throws ReflectiveOperationException {
        Object builder = invoke(ACTION_BUTTON_BUILDER, null, messages.parse("dialog.confirm-button"));
        invoke(void.class, builder.getClass().getMethod("tooltip", Component.class), builder, messages.parse("dialog.confirm-tooltip"));

        Object clickCallbackOptionsBuilder = invoke(CLICK_CALLBACK_OPTIONS_BUILDER, null);
        Object clickCallbackOptions = invoke(CLICK_CALLBACK_OPTIONS_BUILD, clickCallbackOptionsBuilder);

        DialogActionCallback callback = (response, audience) -> handleConfirm(response, audience);

        Object action = invoke(DIALOG_ACTION_CUSTOM_CLICK, null, (DialogActionCallback) callback, clickCallbackOptions);
        invoke(void.class, builder.getClass().getMethod("action", DIALOG_ACTION_CLASS), builder, action);

        return invoke(builder.getClass().getMethod("build"), builder);
    }

    private Object createCancelButton() throws ReflectiveOperationException {
        Object builder = invoke(ACTION_BUTTON_BUILDER, null, messages.parse("dialog.cancel-button"));
        invoke(void.class, builder.getClass().getMethod("tooltip", Component.class), builder, messages.parse("dialog.cancel-tooltip"));

        Object clickCallbackOptionsBuilder = invoke(CLICK_CALLBACK_OPTIONS_BUILDER, null);
        Object clickCallbackOptions = invoke(CLICK_CALLBACK_OPTIONS_BUILD, clickCallbackOptionsBuilder);

        DialogActionCallback callback = (response, audience) -> {
            if (audience instanceof Player player) {
                player.sendMessage(messages.prefixed("dialog.cancelled", Map.of()));
            }
        };

        Object action = invoke(DIALOG_ACTION_CUSTOM_CLICK, null, (DialogActionCallback) callback, clickCallbackOptions);
        invoke(void.class, builder.getClass().getMethod("action", DIALOG_ACTION_CLASS), builder, action);

        return invoke(builder.getClass().getMethod("build"), builder);
    }

    // Helper method to invoke MethodHandle and wrap Throwable
    private Object invoke(MethodHandle handle, Object... args) throws ReflectiveOperationException {
        try {
            return handle.invokeWithArguments(args);
        } catch (Throwable t) {
            throw new ReflectiveOperationException(t);
        }
    }

    // Helper for void methods
    private void invoke(Class<?> returnType, Method method, Object target, Object... args) throws ReflectiveOperationException {
        try {
            method.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw e;
        } catch (Throwable t) {
            throw new ReflectiveOperationException(t);
        }
    }

    private Object invoke(Method method, Object target, Object... args) throws ReflectiveOperationException {
        try {
            return method.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            throw e;
        } catch (Throwable t) {
            throw new ReflectiveOperationException(t);
        }
    }

    @FunctionalInterface
    private interface DialogActionCallback {
        void accept(Object response, Audience audience);
    }

    private void handleConfirm(Object response, Audience audience) {
        if (!(audience instanceof Player player)) return;

        try {
            String dimensionId = (String) invoke(RESPONSE_GET_TEXT, response, "dimension");
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
        } catch (ReflectiveOperationException e) {
            plugin.getLogger().warning("Failed to handle dialog response: " + e.getMessage());
            player.sendMessage(messages.prefixed("error.internal-error", Map.of()));
        }
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
                playDestEffects(bukkitLoc);
                player.sendMessage(messages.prefixed("result.success", Map.of(
                        "dimension", dimension.getName()
                )));
            } else {
                player.sendMessage(messages.prefixed("result.failed-generic", Map.of()));
                cooldownManager.removeCooldown(player.getUniqueId());
            }
        });
    }

    private void playTeleportEffects(Player player, org.bukkit.Location destination) {
        playSourceEffects(player);
        playDestEffects(destination);
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

    private void showFallbackMenu(Player player) {
        player.sendMessage(messages.prefixed("dialog.body", Map.of()));
        player.sendMessage(Component.space());
        for (Dimension dim : Dimension.values()) {
            player.sendMessage(messages.dimensionName(dim));
        }
        player.sendMessage(messages.parseRaw("<gray>Use /rtp <dimension> to teleport (dialog not available)</gray>"));
    }
}