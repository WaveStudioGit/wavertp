package com.wavestudio.rtp.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class CooldownManager {
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();
    private final long cooldownMillis;

    public CooldownManager(long cooldownSeconds) {
        this.cooldownMillis = TimeUnit.SECONDS.toMillis(cooldownSeconds);
    }

    public void updateCooldown(long cooldownSeconds) {
        // Not needed since we calculate dynamically
    }

    public boolean isOnCooldown(UUID playerId) {
        Long lastUse = cooldowns.get(playerId);
        if (lastUse == null) return false;
        return System.currentTimeMillis() - lastUse < cooldownMillis;
    }

    public long getRemainingSeconds(UUID playerId) {
        Long lastUse = cooldowns.get(playerId);
        if (lastUse == null) return 0;
        long remaining = cooldownMillis - (System.currentTimeMillis() - lastUse);
        return Math.max(0, (remaining + 999) / 1000); // Round up
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