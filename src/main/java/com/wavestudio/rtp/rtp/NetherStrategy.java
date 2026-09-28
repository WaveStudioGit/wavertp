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

public class NetherStrategy implements DimensionStrategy {
    private static final int REQUIRED_AIR_BLOCKS = 2;

    @Override
    public String getName() {
        return "Nether";
    }

    @Override
    public CompletableFuture<Optional<Vector>> findSafeLocation(World world, int centerX, int centerZ, int radius, int maxAttempts, RtpConfig config) {
        Map<String, Object> settings = config.getNetherSettings();
        
        int minY = (int) settings.getOrDefault("min-y", 33);
        int maxY = (int) settings.getOrDefault("max-y", 126);
        boolean requireSolidGround = (boolean) settings.getOrDefault("require-solid-ground", true);
        int requiredAir = (int) settings.getOrDefault("required-air-blocks", REQUIRED_AIR_BLOCKS);

        @SuppressWarnings("unchecked")
        List<String> avoidBlockNames = (List<String>) settings.getOrDefault("avoid-blocks", List.of(
            "lava", "fire", "soul_fire", "magma_block"
        ));
        Set<Material> avoidBlocks = parseMaterials(avoidBlockNames);

        return CompletableFuture.supplyAsync(() -> {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            int yRange = maxY - minY + 1;
            
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                int x = centerX + random.nextInt(-radius, radius + 1);
                int z = centerZ + random.nextInt(-radius, radius + 1);
                int y = minY + random.nextInt(yRange);

                if (!isChunkLoaded(world, x, z)) continue;

                Material ground = world.getBlockAt(x, y - 1, z).getType();
                if (requireSolidGround && !isSafeGround(ground, avoidBlocks)) {
                    continue;
                }

                boolean hasAir = true;
                for (int i = 0; i <= requiredAir; i++) {
                    Material at = world.getBlockAt(x, y + i, z).getType();
                    if (!isAir(at) || avoidBlocks.contains(at)) {
                        hasAir = false;
                        break;
                    }
                }

                if (hasAir) {
                    return Optional.of(new Vector(x + 0.5, y, z + 0.5));
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