package com.wavestudio.rtp.config;

import com.wavestudio.rtp.RtpPlugin;
import com.wavestudio.rtp.model.Dimension;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

public class RtpConfig {
    private final RtpPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private File configFile;
    private File messagesFile;

    // Config values
    private int cooldownSeconds;
    private int maxAttempts;
    private int searchRadius;
    private Map<Dimension, String> worldNames;
    private Map<String, Object> overworldSettings;
    private Map<String, Object> netherSettings;
    private Map<String, Object> endSettings;
    private Map<String, Object> effectsSettings;

    public RtpConfig(RtpPlugin plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        // Load config.yml
        configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        // Load messages.yml
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);

        parseConfig();
        plugin.getLogger().info("Configuration loaded");
    }

    private void parseConfig() {
        cooldownSeconds = config.getInt("cooldown-seconds", 10);
        maxAttempts = config.getInt("max-attempts", 20);
        searchRadius = config.getInt("search-radius", 10000);

        worldNames = new EnumMap<>(Dimension.class);
        for (Dimension dim : Dimension.values()) {
            String configured = config.getString("worlds." + dim.getConfigKey(), dim.getDefaultWorldName());
            worldNames.put(dim, configured);
        }

        overworldSettings = config.getConfigurationSection("overworld") != null
                ? config.getConfigurationSection("overworld").getValues(false)
                : Collections.emptyMap();

        netherSettings = config.getConfigurationSection("nether") != null
                ? config.getConfigurationSection("nether").getValues(false)
                : Collections.emptyMap();

        endSettings = config.getConfigurationSection("end") != null
                ? config.getConfigurationSection("end").getValues(false)
                : Collections.emptyMap();

        effectsSettings = config.getConfigurationSection("effects") != null
                ? config.getConfigurationSection("effects").getValues(false)
                : Collections.emptyMap();
    }

    public void reload() {
        load();
    }

    public void save() {
        try {
            config.save(configFile);
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save config", e);
        }
    }

    // Getters
    public int getCooldownSeconds() { return cooldownSeconds; }
    public int getMaxAttempts() { return maxAttempts; }
    public int getSearchRadius() { return searchRadius; }
    public String getWorldName(Dimension dimension) { return worldNames.get(dimension); }
    public Map<String, Object> getOverworldSettings() { return overworldSettings; }
    public Map<String, Object> getNetherSettings() { return netherSettings; }
    public Map<String, Object> getEndSettings() { return endSettings; }
    public Map<String, Object> getEffectsSettings() { return effectsSettings; }

    // Messages
    public String getMessage(String path) {
        String msg = messages.getString(path);
        if (msg == null) {
            plugin.getLogger().warning("Missing message: " + path);
            return path;
        }
        return msg;
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        String msg = getMessage(path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return msg;
    }

    public String getPrefix() {
        return getMessage("prefix");
    }
}