package com.lin.heroic_spirit_spell.util;

import com.lin.heroic_spirit_spell.registry.ModSpells;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Delayed inner-to-outer tremor waves for Magma Tremble.
 */
public final class MagmaTrembleRuntime {
    private static final int WAVE_DELAY_TICKS = 2;
    private static final ConcurrentHashMap<UUID, PendingTremor> PENDING = new ConcurrentHashMap<>();

    private MagmaTrembleRuntime() {
    }

    private record PendingTremor(
            UUID ownerId,
            int ownerEntityId,
            ResourceKey<Level> dimension,
            Vec3 center,
            int radius,
            float damage,
            int igniteTicks,
            float knockbackStrength,
            Set<UUID> hitTargets,
            int currentRing,
            int nextRunTick) {
    }

    public static void start(
            Level level,
            LivingEntity owner,
            Vec3 center,
            int radius,
            float damage,
            int igniteTicks,
            float knockbackStrength) {
        if (level.isClientSide || level.getServer() == null) {
            return;
        }
        int now = level.getServer().getTickCount();
        PENDING.put(owner.getUUID(), new PendingTremor(
                owner.getUUID(),
                owner.getId(),
                level.dimension(),
                center,
                radius,
                damage,
                igniteTicks,
                knockbackStrength,
                new HashSet<>(),
                0,
                now));
    }

    public static void clear(UUID ownerId) {
        PENDING.remove(ownerId);
    }

    public static void tickAll(MinecraftServer server) {
        for (Map.Entry<UUID, PendingTremor> entry : PENDING.entrySet()) {
            PendingTremor pending = entry.getValue();
            if (server.getTickCount() < pending.nextRunTick()) {
                continue;
            }
            ServerLevel level = server.getLevel(pending.dimension());
            if (level == null) {
                PENDING.remove(entry.getKey(), pending);
                continue;
            }

            applyPersistentDamage(level, pending);
            spawnRing(level, pending.center(), pending.radius(), pending.currentRing());
            int nextRing = pending.currentRing() + 1;
            if (nextRing > pending.radius()) {
                PENDING.remove(entry.getKey(), pending);
                continue;
            }
            PENDING.put(entry.getKey(), new PendingTremor(
                    pending.ownerId(),
                    pending.ownerEntityId(),
                    pending.dimension(),
                    pending.center(),
                    pending.radius(),
                    pending.damage(),
                    pending.igniteTicks(),
                    pending.knockbackStrength(),
                    pending.hitTargets(),
                    nextRing,
                    server.getTickCount() + WAVE_DELAY_TICKS));
        }
    }

    private static void applyPersistentDamage(ServerLevel level, PendingTremor pending) {
        Entity ownerEntity = level.getEntity(pending.ownerEntityId());
        if (!(ownerEntity instanceof LivingEntity owner) || !owner.isAlive()) {
            return;
        }
        double radius = pending.radius();
        double radiusSq = radius * radius;
        AABB hitBox = owner.getBoundingBox().inflate(radius, 3.0, radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, hitBox)) {
            if (target == owner || !target.isAlive() || target.isSpectator()) {
                continue;
            }
            if (pending.hitTargets().contains(target.getUUID())) {
                continue;
            }
            if (DamageSources.isFriendlyFireBetween(target, owner)) {
                continue;
            }
            Vec3 delta = target.position().subtract(pending.center());
            if (delta.lengthSqr() > radiusSq) {
                continue;
            }
            DamageSources.applyDamage(target, pending.damage(), ModSpells.MAGMA_TREMBLE.get().getDamageSource(owner));
            target.setRemainingFireTicks(Math.max(target.getRemainingFireTicks(), pending.igniteTicks()));

            Vec3 horizontal = new Vec3(delta.x, 0, delta.z);
            if (horizontal.lengthSqr() < 1.0e-6) {
                horizontal = owner.getLookAngle().multiply(1, 0, 1);
            }
            horizontal = horizontal.normalize().scale(pending.knockbackStrength());
            target.push(horizontal.x, 0.35, horizontal.z);
            target.hurtMarked = true;
            pending.hitTargets().add(target.getUUID());
        }
    }

    private static void spawnRing(ServerLevel level, Vec3 center, int radius, int ring) {
        int cx = BlockPos.containing(center).getX();
        int cz = BlockPos.containing(center).getZ();
        double cy = center.y;
        int ringSqMin = ring == 0 ? 0 : (ring - 1) * (ring - 1);
        int ringSqMax = ring * ring;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int distSq = dx * dx + dz * dz;
                if (distSq > radius * radius || distSq > ringSqMax || distSq < ringSqMin) {
                    continue;
                }
                Vec3 column = new Vec3(cx + dx + 0.5, cy + 2.0, cz + dz + 0.5);
                Vec3 ground = Utils.moveToRelativeGroundLevel(level, column, 12);
                BlockPos below = BlockPos.containing(ground).below();
                Utils.createTremorBlock(level, below, Utils.random.nextFloat() * 0.15f + 0.3f);
            }
        }
    }
}
