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
    public static final int MESSAGES_VERSION = 2;

    public record RankCooldown(String name, String permission, int seconds) {}

    private final RtpPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private File configFile;
    private File messagesFile;

    // Config values
    private int cooldownSeconds;
    private String cooldownBypassPermission;
    private List<RankCooldown> rankCooldowns;
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

        // Load messages.yml (auto-migrate when defaults change)
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        if (messages.getInt("messages-version", 0) < MESSAGES_VERSION) {
            File backup = new File(plugin.getDataFolder(), "messages.yml.bak");
            if (backup.exists()) {
                backup.delete();
            }
            if (!messagesFile.renameTo(backup)) {
                messagesFile.delete();
            }
            plugin.saveResource("messages.yml", false);
            messages = YamlConfiguration.loadConfiguration(messagesFile);
            plugin.getLogger().info("messages.yml updated to v" + MESSAGES_VERSION + " (old file kept as messages.yml.bak)");
        }

        parseConfig();
        plugin.getLogger().info("Configuration loaded");
    }

    private void parseConfig() {
        cooldownSeconds = config.getInt("cooldown-seconds", 5);
        cooldownBypassPermission = config.getString("cooldown-bypass-permission", "wavertp.bypass");

        rankCooldowns = new ArrayList<>();
        if (config.isConfigurationSection("rank-cooldowns")) {
            for (String key : config.getConfigurationSection("rank-cooldowns").getKeys(false)) {
                String perm = config.getString("rank-cooldowns." + key + ".permission", "wavertp.rank." + key);
                int seconds = config.getInt("rank-cooldowns." + key + ".cooldown-seconds", cooldownSeconds);
                rankCooldowns.add(new RankCooldown(key, perm, seconds));
            }
        }

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
    public String getCooldownBypassPermission() { return cooldownBypassPermission; }
    public List<RankCooldown> getRankCooldowns() { return rankCooldowns; }
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