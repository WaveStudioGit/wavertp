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

public class OverworldStrategy implements DimensionStrategy {
    private static final int REQUIRED_AIR_BLOCKS = 2;

    @Override
    public String getName() {
        return "Overworld";
    }

    @Override
    public CompletableFuture<Optional<Vector>> findSafeLocation(World world, int centerX, int centerZ, int radius, int maxAttempts, RtpConfig config) {
        Map<String, Object> settings = config.getOverworldSettings();
        
        @SuppressWarnings("unchecked")
        List<String> avoidBiomeNames = (List<String>) settings.getOrDefault("avoid-biomes", List.of(
            "ocean", "deep_ocean", "lukewarm_ocean", "cold_ocean", "frozen_ocean"
        ));
        Predicate<Biome> biomePredicate = createBiomePredicate(new HashSet<>(avoidBiomeNames));

        @SuppressWarnings("unchecked")
        List<String> avoidBlockNames = (List<String>) settings.getOrDefault("avoid-blocks", List.of(
            "water", "lava", "fire", "soul_fire"
        ));
        Set<Material> avoidBlocks = parseMaterials(avoidBlockNames);

        int requiredAir = (int) settings.getOrDefault("required-air-blocks", REQUIRED_AIR_BLOCKS);

        return CompletableFuture.supplyAsync(() -> {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                int x = centerX + random.nextInt(-radius, radius + 1);
                int z = centerZ + random.nextInt(-radius, radius + 1);

                Chunk chunk = world.getChunkAt(x >> 4, z >> 4);
                if (!chunk.isLoaded()) {
                    continue;
                }

                int y = world.getHighestBlockYAt(x, z);
                if (y <= 0 || y >= world.getMaxHeight() - requiredAir - 1) {
                    continue;
                }

                Biome biome = world.getBiome(x, y, z);
                if (!biomePredicate.test(biome)) {
                    continue;
                }

                Material ground = world.getBlockAt(x, y, z).getType();
                if (!isSafeGround(ground, avoidBlocks)) {
                    continue;
                }

                boolean hasAir = true;
                for (int i = 1; i <= requiredAir; i++) {
                    Material above = world.getBlockAt(x, y + i, z).getType();
                    if (!isAir(above)) {
                        hasAir = false;
                        break;
                    }
                }

                if (hasAir) {
                    return Optional.of(new Vector(x + 0.5, y + 1, z + 0.5));
                }
            }
            return Optional.empty();
        });
    }

    private Set<Material> parseMaterials(List<String> names) {
        Set<Material> materials = new HashSet<>();
        for (String name : names) {
            try {
                Material mat = Material.valueOf(name.toUpperCase(Locale.ROOT));
                materials.add(mat);
            } catch (IllegalArgumentException ignored) {}
        }
        return materials;
    }
}