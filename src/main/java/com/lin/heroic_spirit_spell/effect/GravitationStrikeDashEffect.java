package com.lin.heroic_spirit_spell.effect;

import com.lin.heroic_spirit_spell.registry.ModSpells;
import com.lin.heroic_spirit_spell.util.GravitationStrikeChargeTracker;
import com.lin.heroic_spirit_spell.util.GravitationStrikeDashDisplacement;
import com.lin.heroic_spirit_spell.util.GravitationStrikeDashHitTracker;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.mixin.LivingEntityAccessor;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Dash contact ticks: similar to Iron's BurningDash collision loop; damage uses this spell's {@code getDamageSource} (no fire).
 */
public class GravitationStrikeDashEffect extends MagicMobEffect {

    /** 水平直径 2.5 格 -> 自中心水平半径 2.5；竖直略放宽以覆盖不同体高 */
    private static final double DASH_HIT_RADIUS_XZ = 2.5;
    private static final double DASH_HIT_HALF_HEIGHT = 1.0;

    public GravitationStrikeDashEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean applyEffectTick(LivingEntity livingEntity, int amplifier) {
        Vec3 center = livingEntity.getBoundingBox().getCenter();
        AABB hitBox = new AABB(center, center).inflate(DASH_HIT_RADIUS_XZ, DASH_HIT_HALF_HEIGHT, DASH_HIT_RADIUS_XZ);
        List<Entity> nearby = livingEntity.level().getEntities(livingEntity, hitBox);
        if (!nearby.isEmpty()) {
            float pendingRetribution = livingEntity instanceof ServerPlayer sp
                    ? GravitationStrikeChargeTracker.peekPendingRetribution(sp)
                    : 0f;
            for (Entity entity : nearby) {
                if (entity instanceof LivingEntity target) {
                    if (livingEntity.level().isClientSide()) {
                        continue;
                    }
                    if (!GravitationStrikeDashHitTracker.tryFirstHit(livingEntity, target)) {
                        continue;
                    }
                    int extra = pendingRetribution > 0f ? Math.round(pendingRetribution) : 0;
                    int totalDamage = amplifier + extra;
                    if (DamageSources.applyDamage(
                            entity,
                            totalDamage,
                            ModSpells.GRAVITATION_STRIKE.get().getDamageSource(livingEntity))) {
                        if (extra > 0 && livingEntity instanceof ServerPlayer caster) {
                            GravitationStrikeChargeTracker.clearPendingRetribution(caster);
                            pendingRetribution = 0f;
                        }
                        if (!livingEntity.level().isClientSide()) {
                            GravitationStrikeDashDisplacement.snapTargetAlongDash(livingEntity, target, amplifier);
                            entity.level().playSound(
                                    null,
                                    entity.getX(),
                                    entity.getY(),
                                    entity.getZ(),
                                    SoundRegistry.FIRE_DAGGER_PARRY.get(),
                                    livingEntity.getSoundSource());
                        }
                    }
                    entity.invulnerableTime = 20;
                }
            }
        } else if (livingEntity.horizontalCollision) {
            return false;
        }
        livingEntity.fallDistance = 0;
        return true;
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public void onEffectAdded(LivingEntity livingEntity, int amplifier) {
        super.onEffectAdded(livingEntity, amplifier);
        ((LivingEntityAccessor) livingEntity).setLivingEntityFlagInvoker(4, true);
        if (!livingEntity.level().isClientSide()) {
            GravitationStrikeDashHitTracker.clearSession(livingEntity);
        }
    }

    @Override
    public void onEffectRemoved(LivingEntity livingEntity, int amplifier) {
        super.onEffectRemoved(livingEntity, amplifier);
        ((LivingEntityAccessor) livingEntity).setLivingEntityFlagInvoker(4, false);
        if (!livingEntity.level().isClientSide()) {
            GravitationStrikeDashHitTracker.clearSession(livingEntity);
            if (livingEntity instanceof ServerPlayer sp) {
                GravitationStrikeChargeTracker.clearPendingRetribution(sp);
            }
        }
    }
}
