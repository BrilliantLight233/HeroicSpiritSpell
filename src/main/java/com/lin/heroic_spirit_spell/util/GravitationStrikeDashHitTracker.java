package com.lin.heroic_spirit_spell.util;

import net.minecraft.world.entity.LivingEntity;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One damage resolution per target per dash session (effect ticks re-scan the same entities).
 */
public final class GravitationStrikeDashHitTracker {

    private static final ConcurrentHashMap<UUID, Set<UUID>> DASHER_TO_ALREADY_DAMAGED = new ConcurrentHashMap<>();

    private GravitationStrikeDashHitTracker() {
    }

    public static void clearSession(LivingEntity dasher) {
        DASHER_TO_ALREADY_DAMAGED.remove(dasher.getUUID());
    }

    /**
     * @return true if this is the first time {@code target} is considered for damage this dash
     */
    public static boolean tryFirstHit(LivingEntity dasher, LivingEntity target) {
        if (target == dasher) {
            return false;
        }
        Set<UUID> set = DASHER_TO_ALREADY_DAMAGED.computeIfAbsent(dasher.getUUID(), k -> ConcurrentHashMap.newKeySet());
        return set.add(target.getUUID());
    }
}
