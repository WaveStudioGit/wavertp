package com.wavestudio.rtp.rtp;

import com.wavestudio.rtp.config.RtpConfig;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

public class NetherStrategy implements DimensionStrategy {

    @Override
    public String getName() {
        return "Nether";
    }

    @Override
    public Optional<Vector> check(World world, int x, int z, RtpConfig config) {
        Map<String, Object> settings = config.getNetherSettings();

        int minY = (int) settings.getOrDefault("min-y", 33);
        int maxY = (int) settings.getOrDefault("max-y", 126);
        int requiredAir = (int) settings.getOrDefault("required-air-blocks", 2);

        @SuppressWarnings("unchecked")
        List<String> avoidBlockNames = (List<String>) settings.getOrDefault("avoid-blocks", List.of(
                "lava", "fire", "soul_fire", "magma_block"
        ));
        Set<Material> avoidBlocks = parseMaterials(avoidBlockNames);

        int start = minY + 1 + ThreadLocalRandom.current().nextInt(Math.max(1, maxY - minY));
        Optional<Vector> found = scanRange(world, x, z, start, minY + 1, avoidBlocks, requiredAir);
        if (found.isPresent()) {
            return found;
        }
        if (start < maxY) {
            return scanRange(world, x, z, maxY, start + 1, avoidBlocks, requiredAir);
        }
        return Optional.empty();
    }

    private Optional<Vector> scanRange(World world, int x, int z, int from, int to,
                                       Set<Material> avoidBlocks, int requiredAir) {
        for (int y = from; y >= to; y--) {
            Material ground = world.getBlockAt(x, y - 1, z).getType();
            if (ground == Material.BEDROCK || !isSafeGround(ground, avoidBlocks)) {
                continue;
            }
            boolean clear = true;
            for (int i = 0; i < requiredAir; i++) {
                Material at = world.getBlockAt(x, y + i, z).getType();
                if (!isAir(at) || avoidBlocks.contains(at)) {
                    clear = false;
                    break;
                }
            }
            if (clear) {
                return Optional.of(new Vector(x + 0.5, y, z + 0.5));
            }
        }
        return Optional.empty();
    }

    private Set<Material> parseMaterials(List<String> names) {
        Set<Material> materials = new HashSet<>();
        for (String name : names) {
            try {
                materials.add(Material.valueOf(name.toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {}
        }
        return materials;
    }
}
