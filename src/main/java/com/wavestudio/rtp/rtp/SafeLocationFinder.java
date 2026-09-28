package com.wavestudio.rtp.rtp;

import com.wavestudio.rtp.RtpPlugin;
import com.wavestudio.rtp.config.RtpConfig;
import com.wavestudio.rtp.model.Dimension;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class SafeLocationFinder {
    private final RtpPlugin plugin;
    private final RtpConfig config;
    private final Map<Dimension, DimensionStrategy> strategies;

    public SafeLocationFinder(RtpPlugin plugin, RtpConfig config) {
        this.plugin = plugin;
        this.config = config;
        this.strategies = new EnumMap<>(Dimension.class);
        strategies.put(Dimension.OVERWORLD, new OverworldStrategy());
        strategies.put(Dimension.NETHER, new NetherStrategy());
        strategies.put(Dimension.END, new EndStrategy());
    }

    public CompletableFuture<Optional<Vector>> findSafeLocation(Player player, Dimension dimension) {
        World world = getWorld(dimension);
        if (world == null) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        DimensionStrategy strategy = strategies.get(dimension);
        int radius = config.getSearchRadius();
        int maxAttempts = config.getMaxAttempts();
        int centerX = 0;
        int centerZ = 0;

        plugin.getLogger().info(() -> "Starting RTP search for " + player.getName() + " in " + dimension.getName() + " (radius: " + radius + ", attempts: " + maxAttempts + ")");

        return strategy.findSafeLocation(world, centerX, centerZ, radius, maxAttempts, config)
                .thenApply(result -> {
                    if (result.isPresent()) {
                        plugin.getLogger().info(() -> "Found safe location for " + player.getName() + ": " + result.get());
                    } else {
                        plugin.getLogger().warning(() -> "Failed to find safe location for " + player.getName() + " in " + dimension.getName() + " after " + maxAttempts + " attempts");
                    }
                    return result;
                });
    }

    private World getWorld(Dimension dimension) {
        String worldName = config.getWorldName(dimension);
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            plugin.getLogger().warning("World not found: " + worldName + " for dimension " + dimension.getName());
        }
        return world;
    }

    public void shutdown() {
        strategies.clear();
    }
}