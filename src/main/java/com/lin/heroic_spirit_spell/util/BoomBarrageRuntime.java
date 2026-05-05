package com.lin.heroic_spirit_spell.util;

import io.redspace.ironsspellbooks.entity.spells.fireball.MagicFireball;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Shared boom-barrage projectile helpers: triple shot + 5s lifetime tracking. */
public final class BoomBarrageRuntime {

    public static final float EXPLOSION_RADIUS = 3.0f;
    public static final float PROJECTILE_SPEED = 1.6f;
    private static final int PROJECTILE_LIFETIME_TICKS = 20 * 5;
    private static final float SPLIT_ANGLE_RAD = (float) Math.toRadians(15.0d);

    private static final ConcurrentHashMap<UUID, TrackedProjectile> TRACKED = new ConcurrentHashMap<>();

    private BoomBarrageRuntime() {
    }

    private record TrackedProjectile(ResourceKey<net.minecraft.world.level.Level> dimension, int entityId, long expireGameTime) {
    }

    public static void spawnTriple(
            ServerLevel level,
            LivingEntity owner,
            Vec3 origin,
            Vec3 viewDirection,
            float damage,
            float explosionRadius) {
        Vec3 forward = viewDirection.normalize();
        spawnOne(level, owner, origin, forward, damage, explosionRadius);
        spawnOne(level, owner, origin, forward.yRot(SPLIT_ANGLE_RAD), damage, explosionRadius);
        spawnOne(level, owner, origin, forward.yRot(-SPLIT_ANGLE_RAD), damage, explosionRadius);
    }

    private static void spawnOne(
            ServerLevel level,
            LivingEntity owner,
            Vec3 origin,
            Vec3 direction,
            float damage,
            float explosionRadius) {
        MagicFireball fireball = new MagicFireball(level, owner);
        fireball.setDamage(damage);
        fireball.setExplosionRadius(explosionRadius);
        fireball.setPos(origin.x, origin.y, origin.z);
        fireball.shoot(direction);
        fireball.setDeltaMovement(fireball.getDeltaMovement().normalize().scale(PROJECTILE_SPEED));
        level.addFreshEntity(fireball);
        TRACKED.put(
                fireball.getUUID(),
                new TrackedProjectile(level.dimension(), fireball.getId(), level.getGameTime() + PROJECTILE_LIFETIME_TICKS));
    }

    public static void tickAll(MinecraftServer server) {
        for (Map.Entry<UUID, TrackedProjectile> entry : TRACKED.entrySet()) {
            TrackedProjectile tracked = entry.getValue();
            ServerLevel level = server.getLevel(tracked.dimension());
            if (level == null) {
                TRACKED.remove(entry.getKey(), tracked);
                continue;
            }
            if (level.getGameTime() < tracked.expireGameTime()) {
                continue;
            }
            Entity entity = level.getEntity(tracked.entityId());
            if (entity instanceof MagicFireball fireball && fireball.isAlive()) {
                fireball.discard();
            }
            TRACKED.remove(entry.getKey(), tracked);
        }
    }
}
