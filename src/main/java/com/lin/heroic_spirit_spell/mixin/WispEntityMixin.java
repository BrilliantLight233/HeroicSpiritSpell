package com.lin.heroic_spirit_spell.mixin;

import com.lin.heroic_spirit_spell.util.HolyFortifyOverflow;
import io.redspace.ironsspellbooks.entity.spells.wisp.WispEntity;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;

@Mixin(WispEntity.class)
public abstract class WispEntityMixin extends PathfinderMob {

    @Shadow
    private UUID ownerUUID;
    @Shadow
    private Entity cachedOwner;

    protected WispEntityMixin(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    // 1. 提高圣灵的移动速度
    @Inject(method = "<init>(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;)V", at = @At("TAIL"))
    private void heroicSpiritSpell$increaseSpeed(EntityType<? extends WispEntity> entityType, Level level,
            CallbackInfo ci) {
        // 原版 MOVEMENT_SPEED 和 FLYING_SPEED 都是 0.2，这里提高到 0.4
        if (this.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.4d);
        }
        if (this.getAttribute(Attributes.FLYING_SPEED) != null) {
            this.getAttribute(Attributes.FLYING_SPEED).setBaseValue(0.4d);
        }
    }

    // 2. 无锁定释放追踪逻辑
    @Inject(method = "tick", at = @At("HEAD"))
    private void heroicSpiritSpell$autoTracking(CallbackInfo ci) {
        if (this.level().isClientSide)
            return;

        // 如果没有目标，自动搜索附近的目标
        if (this.getTarget() == null) {
            // 搜索 32 格内的潜在目标
            AABB searchBox = this.getBoundingBox().inflate(32.0);
            List<LivingEntity> potentialTargets = this.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                    e -> e != this && e != cachedOwner && e.isAlive() && !e.isSpectator());

            LivingEntity bestTarget = null;
            double closestDistSqr = Double.MAX_VALUE;

            for (LivingEntity target : potentialTargets) {
                // 判断是否是盟友
                boolean targetIsAlly = (cachedOwner != null && target.isAlliedTo(cachedOwner));
                boolean bestIsAlly = bestTarget != null && cachedOwner != null && bestTarget.isAlliedTo(cachedOwner);

                double distSqr = this.distanceToSqr(target);

                if (bestTarget == null) {
                    bestTarget = target;
                    closestDistSqr = distSqr;
                } else {
                    // 优先选择非盟友（敌人）
                    if (!targetIsAlly && bestIsAlly) {
                        bestTarget = target;
                        closestDistSqr = distSqr;
                    } else if (targetIsAlly == bestIsAlly) {
                        // 如果同为盟友或同为敌人，选择更近的
                        if (distSqr < closestDistSqr) {
                            bestTarget = target;
                            closestDistSqr = distSqr;
                        }
                    }
                }
            }

            if (bestTarget != null) {
                this.setTarget(bestTarget);
            }
        }
    }

    // 3. 命中队友时的治疗与护盾逻辑
    // 通过 Redirect 拦截 AABB.intersects 调用，因为这是原版 tick 中判断是否命中的关键逻辑
    @Redirect(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;intersects(Lnet/minecraft/world/phys/AABB;)Z"))
    private boolean heroicSpiritSpell$handleCollision(AABB instance, AABB other) {
        boolean intersects = instance.intersects(other);

        // 如果发生了碰撞，且在服务端
        if (intersects && !this.level().isClientSide) {
            LivingEntity target = this.getTarget();
            // 检查目标是否是队友
            if (target != null && cachedOwner != null && target.isAlliedTo(cachedOwner)) {
                // 执行治疗逻辑
                float healAmount = 10.0f;
                float currentHealth = target.getHealth();
                float maxHealth = target.getMaxHealth();
                float missingHealth = maxHealth - currentHealth;

                target.heal(healAmount);

                HolyFortifyOverflow.applyOverflowFortify(target, healAmount - missingHealth);

                // 触发移除逻辑（这将触发下面的 remove Mixin，产生消失特效）
                this.discard();

                // 返回 false 欺骗原版逻辑，使其认为没有碰撞，从而跳过原版的伤害处理
                return false;
            }
        }
        return intersects;
    }
}
