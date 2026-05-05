package com.lin.heroic_spirit_spell.mixin;

import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.entity.spells.lightning_lance.LightningLanceProjectile;
import io.redspace.ironsspellbooks.entity.spells.magic_arrow.MagicArrowProjectile;
import io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile;
import io.redspace.ironsspellbooks.entity.spells.icicle.IcicleProjectile;
import io.redspace.ironsspellbooks.entity.spells.fire_arrow.FireArrowProjectile;
import io.redspace.ironsspellbooks.entity.spells.firebolt.FireboltProjectile;
import io.redspace.ironsspellbooks.entity.spells.fireball.SmallMagicFireball;
import io.redspace.ironsspellbooks.entity.spells.poison_arrow.PoisonArrow;
import io.redspace.ironsspellbooks.entity.spells.guiding_bolt.GuidingBoltProjectile;
import io.redspace.ironsspellbooks.entity.spells.blood_needle.BloodNeedle;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.phys.Vec3;

@Mixin(AbstractMagicProjectile.class)
public class MagicProjectileSpeedMixin {
    private Vec3 heroicSpiritSpell$spawnLockOrigin;
    private Vec3 heroicSpiritSpell$spawnLockVelocity;
    private int heroicSpiritSpell$spawnLockStartTick = -1;
    private int heroicSpiritSpell$spawnLockEndTick = -1;

    @ModifyConstant(method = "handleCursorHoming", constant = @Constant(floatValue = 48.0f), remap = false)
    private float heroicSpiritSpell$modifyCursorHomingRange(float range) {
        if ((Object) this instanceof SmallMagicFireball) {
            return 128.0f; // Increase tracking range from 48 to 128
        }
        return range;
    }

    private double heroicSpiritSpell$getCustomSpeed(AbstractMagicProjectile projectile) {
        if (projectile instanceof LightningLanceProjectile)
            return 6.4d;
        if (projectile instanceof MagicArrowProjectile)
            return 6.4d;
        if (projectile instanceof MagicMissileProjectile)
            return 4.8d;
        if (projectile instanceof IcicleProjectile)
            return 4.8d;
        // 火焰箭 (Firebolt) -> 6.4
        if (projectile instanceof FireboltProjectile)
            return 4.8d;
        // 爆裂炽炎箭 (Explosive Fire Arrow) -> 4.8
        if (projectile instanceof FireArrowProjectile)
            return 3.2d;
        // 炽焰追踪弹幕 (Flaming Barrage) -> 3.2
        if (projectile instanceof SmallMagicFireball)
            return 3.2d;
        if (projectile instanceof PoisonArrow)
            return 3.2d;
        if (projectile instanceof GuidingBoltProjectile)
            return 2.4d;
        if (projectile instanceof BloodNeedle)
            return 2.4d;
        return -1d;
    }

    private boolean heroicSpiritSpell$shouldUsePreciseSpawnVelocity(AbstractMagicProjectile projectile) {
        return heroicSpiritSpell$getCustomSpeed(projectile) > 3.9d;
    }

    @Inject(method = "shoot", at = @At("TAIL"), remap = false)
    private void heroicSpiritSpell$setProjectileShootSpeed(Vec3 direction, CallbackInfo ci) {
        AbstractMagicProjectile projectile = (AbstractMagicProjectile) (Object) this;
        double speed = heroicSpiritSpell$getCustomSpeed(projectile);
        if (speed > 0) {
            projectile.setDeltaMovement(direction.normalize().scale(speed));
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), remap = false)
    private void heroicSpiritSpell$syncProjectileFirstTickVelocity(CallbackInfo ci) {
        AbstractMagicProjectile projectile = (AbstractMagicProjectile) (Object) this;
        if (projectile.tickCount != 0 || projectile.level().isClientSide) {
            return;
        }
        double speed = heroicSpiritSpell$getCustomSpeed(projectile);
        if (speed > 0) {
            Vec3 deltaMovement = projectile.getDeltaMovement();
            if (deltaMovement.lengthSqr() > 1.0e-7d) {
                projectile.setDeltaMovement(deltaMovement.normalize().scale(speed));
            }
        }
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void heroicSpiritSpell$lockClientSpawnPath(CallbackInfo ci) {
        AbstractMagicProjectile projectile = (AbstractMagicProjectile) (Object) this;
        if (!projectile.level().isClientSide) {
            return;
        }
        if (heroicSpiritSpell$spawnLockVelocity == null || heroicSpiritSpell$spawnLockOrigin == null) {
            return;
        }
        if (heroicSpiritSpell$spawnLockStartTick < 0 || projectile.tickCount > heroicSpiritSpell$spawnLockEndTick) {
            heroicSpiritSpell$spawnLockVelocity = null;
            heroicSpiritSpell$spawnLockOrigin = null;
            return;
        }
        int deltaTicks = projectile.tickCount - heroicSpiritSpell$spawnLockStartTick + 1;
        if (deltaTicks <= 0) {
            return;
        }
        Vec3 expected = heroicSpiritSpell$spawnLockOrigin.add(heroicSpiritSpell$spawnLockVelocity.scale(deltaTicks));
        projectile.setPos(expected);
        projectile.setDeltaMovement(heroicSpiritSpell$spawnLockVelocity);
        projectile.setOldPosAndRot();
    }

    @Inject(method = "writeSpawnData", at = @At("TAIL"), remap = false)
    private void heroicSpiritSpell$writePreciseSpawnVelocity(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
        AbstractMagicProjectile projectile = (AbstractMagicProjectile) (Object) this;
        boolean usePrecise = heroicSpiritSpell$shouldUsePreciseSpawnVelocity(projectile);
        buffer.writeBoolean(usePrecise);
        if (!usePrecise) {
            return;
        }
        Vec3 deltaMovement = projectile.getDeltaMovement();
        if (deltaMovement.lengthSqr() <= 1.0e-7d) {
            buffer.writeDouble(0d);
            buffer.writeDouble(0d);
            buffer.writeDouble(0d);
            return;
        }
        Vec3 direction = deltaMovement.normalize();
        double speed = heroicSpiritSpell$getCustomSpeed(projectile);
        buffer.writeDouble(direction.x * speed);
        buffer.writeDouble(direction.y * speed);
        buffer.writeDouble(direction.z * speed);
    }

    @Inject(method = "readSpawnData", at = @At("TAIL"))
    private void heroicSpiritSpell$fixSpawnVelocityDirection(RegistryFriendlyByteBuf buffer, CallbackInfo ci) {
        AbstractMagicProjectile projectile = (AbstractMagicProjectile) (Object) this;
        if (!projectile.level().isClientSide) {
            return;
        }
        boolean usePrecise = buffer.readBoolean();
        if (!usePrecise) {
            return;
        }
        double vx = buffer.readDouble();
        double vy = buffer.readDouble();
        double vz = buffer.readDouble();
        Vec3 movement = new Vec3(vx, vy, vz);
        if (movement.lengthSqr() > 1.0e-7d) {
            projectile.setDeltaMovement(movement);
            projectile.setOldPosAndRot();
            heroicSpiritSpell$spawnLockOrigin = projectile.position();
            heroicSpiritSpell$spawnLockVelocity = movement;
            heroicSpiritSpell$spawnLockStartTick = projectile.tickCount;
            heroicSpiritSpell$spawnLockEndTick = projectile.tickCount + 2;
        }
    }
}
