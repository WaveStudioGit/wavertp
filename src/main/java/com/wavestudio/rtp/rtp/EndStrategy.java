package com.wavestudio.rtp.rtp;

import com.wavestudio.rtp.config.RtpConfig;
import com.wavestudio.rtp.model.Dimension;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Locale;

public class EndStrategy implements DimensionStrategy {
    private static final int REQUIRED_AIR_BLOCKS = 2;

    @Override
    public String getName() {
        return "The End";
    }

    @Override
    public CompletableFuture<Optional<Vector>> findSafeLocation(World world, int centerX, int centerZ, int radius, int maxAttempts, RtpConfig config) {
        Map<String, Object> settings = config.getEndSettings();
        
        int minY = (int) settings.getOrDefault("min-y", 1);
        int requiredAir = (int) settings.getOrDefault("required-air-blocks", REQUIRED_AIR_BLOCKS);

        @SuppressWarnings("unchecked")
        List<String> validGroundNames = (List<String>) settings.getOrDefault("valid-ground-blocks", List.of(
            "end_stone", "end_stone_bricks", "purpur_block"
        ));
        Set<Material> validGround = parseMaterials(validGroundNames);

        @SuppressWarnings("unchecked")
        List<String> avoidBlockNames = (List<String>) settings.getOrDefault("avoid-blocks", List.of(
            "void_air"
        ));
        Set<Material> avoidBlocks = parseMaterials(avoidBlockNames);

        return CompletableFuture.supplyAsync(() -> {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                int x = centerX + random.nextInt(-radius, radius + 1);
                int z = centerZ + random.nextInt(-radius, radius + 1);

                if (!isChunkLoaded(world, x, z)) continue;

                int y = world.getHighestBlockYAt(x, z);
                if (y < minY) continue;

                Material ground = world.getBlockAt(x, y, z).getType();
                if (!validGround.contains(ground)) continue;

                boolean hasAir = true;
                for (int i = 1; i <= requiredAir; i++) {
                    Material above = world.getBlockAt(x, y + i, z).getType();
                    if (!isAir(above) || avoidBlocks.contains(above)) {
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

    private boolean isChunkLoaded(World world, int x, int z) {
        return world.isChunkLoaded(x >> 4, z >> 4);
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