package com.wavestudio.rtp.util;

import com.wavestudio.rtp.config.RtpConfig;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CooldownManager {
    private final RtpConfig config;
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public CooldownManager(RtpConfig config) {
        this.config = config;
    }

    public int getCooldownSeconds(Player player) {
        if (player.hasPermission(config.getCooldownBypassPermission())) {
            return 0;
        }
        int best = config.getCooldownSeconds();
        for (RtpConfig.RankCooldown rank : config.getRankCooldowns()) {
            if (player.hasPermission(rank.permission())) {
                best = Math.min(best, rank.seconds());
            }
        }
        return Math.max(0, best);
    }

    public boolean isOnCooldown(Player player) {
        int seconds = getCooldownSeconds(player);
        if (seconds <= 0) return false;
        Long lastUse = cooldowns.get(player.getUniqueId());
        if (lastUse == null) return false;
        return System.currentTimeMillis() - lastUse < TimeUnit.SECONDS.toMillis(seconds);
    }

    public long getRemainingSeconds(Player player) {
        int seconds = getCooldownSeconds(player);
        Long lastUse = cooldowns.get(player.getUniqueId());
        if (lastUse == null) return 0;
        long remaining = TimeUnit.SECONDS.toMillis(seconds) - (System.currentTimeMillis() - lastUse);
        return Math.max(0, (remaining + 999) / 1000);
    }

    public void setCooldown(UUID playerId) {
        cooldowns.put(playerId, System.currentTimeMillis());
    }

    public void removeCooldown(UUID playerId) {
        cooldowns.remove(playerId);
    }

    public void clear() {
        cooldowns.clear();
    }
}
