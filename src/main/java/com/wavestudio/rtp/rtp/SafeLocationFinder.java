package com.wavestudio.rtp.rtp;

import com.wavestudio.rtp.RtpPlugin;
import com.wavestudio.rtp.config.RtpConfig;
import com.wavestudio.rtp.model.Dimension;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

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

        plugin.getLogger().info(() -> "Starting RTP search for " + player.getName()
                + " in " + dimension.getName() + " (radius: " + radius + ", attempts: " + maxAttempts + ")");

        CompletableFuture<Optional<Vector>> result = new CompletableFuture<>();
        attempt(player, world, strategy, dimension, radius, 0, maxAttempts, result);
        return result.thenApply(found -> {
            if (found.isPresent()) {
                plugin.getLogger().info(() -> "Found safe location for " + player.getName() + ": " + found.get());
            } else {
                plugin.getLogger().warning(() -> "Failed to find safe location for " + player.getName()
                        + " in " + dimension.getName() + " after " + maxAttempts + " attempts");
            }
            return found;
        });
    }

    private void attempt(Player player, World world, DimensionStrategy strategy, Dimension dimension,
                         int radius, int index, int maxAttempts, CompletableFuture<Optional<Vector>> result) {
        if (result.isDone() || index >= maxAttempts || !player.isOnline()) {
            result.complete(Optional.empty());
            return;
        }

        int[] xz = pickCoords(dimension, radius);
        int x = xz[0];
        int z = xz[1];
        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        try {
            world.getChunkAtAsync(chunkX, chunkZ, true).thenAccept(chunk -> {
                runOnRegion(world, chunkX, chunkZ, () -> {
                    if (result.isDone() || !player.isOnline()) {
                        result.complete(Optional.empty());
                        return;
                    }
                    try {
                        Optional<Vector> found = strategy.check(world, x, z, config);
                        if (found.isPresent()) {
                            result.complete(found);
                        } else {
                            attempt(player, world, strategy, dimension, radius, index + 1, maxAttempts, result);
                        }
                    } catch (Exception e) {
                        attempt(player, world, strategy, dimension, radius, index + 1, maxAttempts, result);
                    }
                });
            }).exceptionally(ex -> {
                runOnRegion(world, chunkX, chunkZ, () ->
                        attempt(player, world, strategy, dimension, radius, index + 1, maxAttempts, result));
                return null;
            });
        } catch (Exception e) {
            runOnRegion(world, chunkX, chunkZ, () ->
                    attempt(player, world, strategy, dimension, radius, index + 1, maxAttempts, result));
        }
    }

    private void runOnRegion(World world, int chunkX, int chunkZ, Runnable task) {
        try {
            Bukkit.getRegionScheduler().run(plugin, world, chunkX, chunkZ, scheduled -> task.run());
        } catch (Exception e) {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }

    private int[] pickCoords(Dimension dimension, int radius) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        if (dimension == Dimension.END) {
            if (random.nextInt(10) < 7) {
                int bound = 300;
                return new int[]{random.nextInt(-bound, bound + 1), random.nextInt(-bound, bound + 1)};
            }
            double angle = random.nextDouble(Math.PI * 2);
            int dist = 900 + random.nextInt(1600);
            return new int[]{(int) (Math.cos(angle) * dist), (int) (Math.sin(angle) * dist)};
        }
        return new int[]{random.nextInt(-radius, radius + 1), random.nextInt(-radius, radius + 1)};
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
