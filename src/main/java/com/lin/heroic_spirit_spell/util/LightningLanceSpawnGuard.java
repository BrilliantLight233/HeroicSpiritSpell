package com.lin.heroic_spirit_spell.util;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks last lightning lance spawn tick per player. Plain Java class; safe to call from mixins.
 */
public final class LightningLanceSpawnGuard {

    private static final ConcurrentHashMap<UUID, Long> LAST_SPAWN_GAME_TICK = new ConcurrentHashMap<>();

    private LightningLanceSpawnGuard() {
    }

    public static void markSpawn(ServerPlayer player) {
        LAST_SPAWN_GAME_TICK.put(player.getUUID(), player.level().getGameTime());
    }

    public static long ticksSinceLastSpawn(UUID playerId, long gameTime) {
        Long last = LAST_SPAWN_GAME_TICK.get(playerId);
        return last == null ? Long.MAX_VALUE : gameTime - last;
    }

    public static void clear(UUID playerId) {
        LAST_SPAWN_GAME_TICK.remove(playerId);
    }
}
