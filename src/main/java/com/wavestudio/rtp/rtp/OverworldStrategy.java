package com.wavestudio.rtp.rtp;

import com.wavestudio.rtp.config.RtpConfig;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public class OverworldStrategy implements DimensionStrategy {

    @Override
    public String getName() {
        return "Overworld";
    }

    @Override
    public Optional<Vector> check(World world, int x, int z, RtpConfig config) {
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

        int requiredAir = (int) settings.getOrDefault("required-air-blocks", 2);

        int y = world.getHighestBlockYAt(x, z);
        if (y <= 0 || y >= world.getMaxHeight() - requiredAir - 1) {
            return Optional.empty();
        }

        Biome biome = world.getBiome(x, y, z);
        if (!biomePredicate.test(biome)) {
            return Optional.empty();
        }

        Material ground = world.getBlockAt(x, y, z).getType();
        if (!isSafeGround(ground, avoidBlocks)) {
            return Optional.empty();
        }

        for (int i = 1; i <= requiredAir; i++) {
            if (!isAir(world.getBlockAt(x, y + i, z).getType())) {
                return Optional.empty();
            }
        }

        return Optional.of(new Vector(x + 0.5, y + 1, z + 0.5));
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
