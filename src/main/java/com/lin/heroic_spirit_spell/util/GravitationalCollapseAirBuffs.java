package com.lin.heroic_spirit_spell.util;

import com.lin.heroic_spirit_spell.registry.ModEffects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * After antigravity (10t), applies spell gravity II for 10t at a scheduled server tick.
 */
public final class GravitationalCollapseAirBuffs {

    private static final Map<UUID, Integer> APPLY_SPELL_GRAVITY_AT_TICK = new ConcurrentHashMap<>();

    private GravitationalCollapseAirBuffs() {
    }

    public static void armSpellGravityPhase(LivingEntity entity, int serverTickWhen) {
        APPLY_SPELL_GRAVITY_AT_TICK.put(entity.getUUID(), serverTickWhen);
    }

    /** Once per server tick: apply delayed spell gravity to armed entities. */
    public static void tickDue(MinecraftServer server) {
        if (APPLY_SPELL_GRAVITY_AT_TICK.isEmpty()) {
            return;
        }
        int now = server.getTickCount();
        for (UUID id : new ArrayList<>(APPLY_SPELL_GRAVITY_AT_TICK.keySet())) {
            Integer when = APPLY_SPELL_GRAVITY_AT_TICK.get(id);
            if (when == null || now < when) {
                continue;
            }
            LivingEntity le = findLiving(server, id);
            if (le == null || le.isRemoved() || !le.isAlive()) {
                APPLY_SPELL_GRAVITY_AT_TICK.remove(id);
                continue;
            }
            le.addEffect(new MobEffectInstance(ModEffects.SPELL_GRAVITY, 10, 1, false, true, true));
            APPLY_SPELL_GRAVITY_AT_TICK.remove(id);
        }
    }

    @Nullable
    private static LivingEntity findLiving(MinecraftServer server, UUID id) {
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player != null) {
            return player;
        }
        for (ServerLevel sl : server.getAllLevels()) {
            for (Entity e : sl.getAllEntities()) {
                if (e instanceof LivingEntity le && id.equals(e.getUUID())) {
                    return le;
                }
            }
        }
        return null;
    }

    public static void clear(UUID entityId) {
        APPLY_SPELL_GRAVITY_AT_TICK.remove(entityId);
    }
}
