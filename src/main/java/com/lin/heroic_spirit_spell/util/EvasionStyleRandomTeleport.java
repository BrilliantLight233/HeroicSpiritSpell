package com.lin.heroic_spirit_spell.util;

import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * Port of IronsSpellbooks EvasionEffect random teleport loop (evasion spell).
 */
public final class EvasionStyleRandomTeleport {

    private EvasionStyleRandomTeleport() {
    }

    public static void teleportNear(LivingEntity livingEntity) {
        if (livingEntity.level().isClientSide || !(livingEntity.level() instanceof ServerLevel level)) {
            return;
        }

        double d0 = livingEntity.getX();
        double d1 = livingEntity.getY();
        double d2 = livingEntity.getZ();
        double maxRadius = 12d;
        var random = livingEntity.getRandom();

        for (int i = 0; i < 16; ++i) {
            var minRadius = maxRadius / 2;
            Vec3 vec = new Vec3(random.nextInt((int) minRadius, (int) maxRadius), 0, 0);
            int degrees = random.nextInt(360);
            vec = vec.yRot(degrees * Mth.DEG_TO_RAD);

            double x = d0 + vec.x;
            double y = Mth.clamp(
                    livingEntity.getY() + (double) (livingEntity.getRandom().nextInt((int) maxRadius) - maxRadius / 2),
                    (double) level.getMinBuildHeight(),
                    (double) (level.getMinBuildHeight() + level.getLogicalHeight() - 1));
            double z = d2 + vec.z;

            if (livingEntity.isPassenger()) {
                livingEntity.stopRiding();
            }

            if (livingEntity.randomTeleport(x, y, z, true)) {
                level.playSound(null, d0, d1, d2, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                livingEntity.playSound(SoundEvents.ENDERMAN_TELEPORT, 2.0F, 1.0F);
                break;
            }

            if (maxRadius > 2) {
                maxRadius--;
            }
        }

        particleCloud(livingEntity);
    }

    private static void particleCloud(LivingEntity entity) {
        Vec3 pos = entity.position().add(0, entity.getBbHeight() / 2, 0);
        MagicManager.spawnParticles(
                entity.level(),
                ParticleTypes.PORTAL,
                pos.x,
                pos.y,
                pos.z,
                70,
                entity.getBbWidth() / 4,
                entity.getBbHeight() / 5,
                entity.getBbWidth() / 4,
                .035,
                false);
    }

    public static void lookTowardSource(ServerPlayer player, net.minecraft.world.damagesource.DamageSource damageSource) {
        if (damageSource != null && damageSource.getEntity() != null) {
            player.lookAt(EntityAnchorArgument.Anchor.EYES, damageSource.getEntity().getEyePosition());
        }
    }
}
