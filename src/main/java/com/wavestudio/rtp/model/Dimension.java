package com.wavestudio.rtp.model;

import org.bukkit.NamespacedKey;
import org.bukkit.World;

import java.util.Locale;

public enum Dimension {
    OVERWORLD("overworld", "world", World.Environment.NORMAL, 0x00AA00),
    NETHER("nether", "world_nether", World.Environment.NETHER, 0xAA0000),
    END("end", "world_the_end", World.Environment.THE_END, 0x00AAAA);

    private final String id;
    private final String defaultWorldName;
    private final World.Environment environment;
    private final int color;

    Dimension(String id, String defaultWorldName, World.Environment environment, int color) {
        this.id = id;
        this.defaultWorldName = defaultWorldName;
        this.environment = environment;
        this.color = color;
    }

    public String getId() {
        return id;
    }

    public String getDefaultWorldName() {
        return defaultWorldName;
    }

    public World.Environment getEnvironment() {
        return environment;
    }

    public int getColor() {
        return color;
    }

    public NamespacedKey getNamespacedKey() {
        return NamespacedKey.minecraft(id);
    }

    public static Dimension fromId(String id) {
        for (Dimension dim : values()) {
            if (dim.id.equalsIgnoreCase(id)) {
                return dim;
            }
        }
        return null;
    }

    public static Dimension fromEnvironment(World.Environment env) {
        for (Dimension dim : values()) {
            if (dim.environment == env) {
                return dim;
            }
        }
        return OVERWORLD;
    }

    public String getConfigKey() {
        return id.toLowerCase(Locale.ROOT);
    }
}