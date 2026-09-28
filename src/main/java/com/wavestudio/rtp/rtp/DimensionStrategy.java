package com.wavestudio.rtp.rtp;

import com.wavestudio.rtp.config.RtpConfig;
import com.wavestudio.rtp.model.Dimension;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.data.BlockData;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;
import java.util.Locale;

public interface DimensionStrategy {
    String getName();

    CompletableFuture<Optional<Vector>> findSafeLocation(World world, int centerX, int centerZ, int radius, int maxAttempts, RtpConfig config);

    default boolean isSafeGround(Material material, Set<Material> avoidBlocks) {
        return material != null && material.isSolid() && !avoidBlocks.contains(material);
    }

    default boolean isAir(Material material) {
        return material == null || material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
    }

    default boolean isLiquid(Material material) {
        return material != null && (material == Material.WATER || material == Material.LAVA);
    }

    default Predicate<Biome> createBiomePredicate(Set<String> avoidBiomeNames) {
        Set<Biome> avoidBiomes = new HashSet<>();
        for (String name : avoidBiomeNames) {
            try {
                Biome biome = Biome.valueOf(name.toUpperCase(Locale.ROOT));
                avoidBiomes.add(biome);
            } catch (IllegalArgumentException ignored) {}
        }
        return biome -> !avoidBiomes.contains(biome);
    }
}