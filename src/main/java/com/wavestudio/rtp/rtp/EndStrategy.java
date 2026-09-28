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

public class EndStrategy implements DimensionStrategy {

    @Override
    public String getName() {
        return "The End";
    }

    @Override
    public Optional<Vector> check(World world, int x, int z, RtpConfig config) {
        Map<String, Object> settings = config.getEndSettings();

        int minY = (int) settings.getOrDefault("min-y", 1);
        int requiredAir = (int) settings.getOrDefault("required-air-blocks", 2);

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

        int y = world.getHighestBlockYAt(x, z);
        if (y < minY) {
            return Optional.empty();
        }

        Material ground = world.getBlockAt(x, y, z).getType();
        if (!validGround.contains(ground)) {
            return Optional.empty();
        }

        for (int i = 1; i <= requiredAir; i++) {
            Material above = world.getBlockAt(x, y + i, z).getType();
            if (!isAir(above) || avoidBlocks.contains(above)) {
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
