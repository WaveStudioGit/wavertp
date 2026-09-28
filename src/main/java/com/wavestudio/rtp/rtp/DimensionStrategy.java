package com.wavestudio.rtp.rtp;

import com.wavestudio.rtp.config.RtpConfig;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

public interface DimensionStrategy {
    String getName();

    Optional<Vector> check(World world, int x, int z, RtpConfig config);

    default boolean isSafeGround(Material material, Set<Material> avoidBlocks) {
        return material != null && material.isSolid() && !avoidBlocks.contains(material);
    }

    default boolean isAir(Material material) {
        return material == null || material == Material.AIR || material == Material.CAVE_AIR || material == Material.VOID_AIR;
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
