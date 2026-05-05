package com.lin.heroic_spirit_spell.util;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side: wait until caster lands, then run landing AoE once. */
public final class GravitationalCollapsePending {

    public record LandingData(int spellLevel, int expireGameTime) {
    }

    private static final ConcurrentHashMap<UUID, LandingData> BY_PLAYER = new ConcurrentHashMap<>();

    private GravitationalCollapsePending() {
    }

    public static void arm(UUID playerId, int spellLevel, int expireGameTime) {
        BY_PLAYER.put(playerId, new LandingData(spellLevel, expireGameTime));
    }

    public static LandingData get(UUID playerId) {
        return BY_PLAYER.get(playerId);
    }

    public static void clear(UUID playerId) {
        BY_PLAYER.remove(playerId);
    }
}
