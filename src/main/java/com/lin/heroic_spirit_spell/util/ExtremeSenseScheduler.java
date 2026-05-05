package com.lin.heroic_spirit_spell.util;

import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ExtremeSenseScheduler {

    /** Teleport was 40 tick; now +5 tick */
    public static final int TELEPORT_DELAY_TICKS = 41;

    /** Ticks after proc before applying Heartstop */
    public static final int HEARTSTOP_APPLY_DELAY_TICKS = 1;

    /** Heartstop duration once applied */
    public static final int HEARTSTOP_DURATION_TICKS = 40;

    private static final Map<UUID, Integer> PENDING_HEARTSTOP = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> PENDING_TELEPORT = new ConcurrentHashMap<>();

    private ExtremeSenseScheduler() {
    }

    public static void arm(ServerPlayer player) {
        PENDING_HEARTSTOP.put(player.getUUID(), HEARTSTOP_APPLY_DELAY_TICKS);
        PENDING_TELEPORT.put(player.getUUID(), TELEPORT_DELAY_TICKS);
    }

    public static void clear(ServerPlayer player) {
        UUID id = player.getUUID();
        PENDING_HEARTSTOP.remove(id);
        PENDING_TELEPORT.remove(id);
    }

    public static boolean hasPending(ServerPlayer player) {
        UUID id = player.getUUID();
        return PENDING_HEARTSTOP.containsKey(id) || PENDING_TELEPORT.containsKey(id);
    }

    public static void tick(ServerPlayer player) {
        if (player.level().isClientSide()) {
            return;
        }
        UUID id = player.getUUID();

        Integer hLeft = PENDING_HEARTSTOP.get(id);
        if (hLeft != null) {
            if (hLeft <= 1) {
                PENDING_HEARTSTOP.remove(id);
                if (player.isAlive()) {
                    player.addEffect(new MobEffectInstance(
                            MobEffectRegistry.HEARTSTOP, HEARTSTOP_DURATION_TICKS, 0, false, false, true));
                }
            } else {
                PENDING_HEARTSTOP.put(id, hLeft - 1);
            }
        }

        Integer tLeft = PENDING_TELEPORT.get(id);
        if (tLeft != null) {
            if (tLeft <= 1) {
                PENDING_TELEPORT.remove(id);
                if (player.isAlive()) {
                    EvasionStyleRandomTeleport.teleportNear(player);
                }
            } else {
                PENDING_TELEPORT.put(id, tLeft - 1);
            }
        }
    }

    public static void onLogout(UUID uuid) {
        PENDING_HEARTSTOP.remove(uuid);
        PENDING_TELEPORT.remove(uuid);
    }
}
