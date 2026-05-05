package com.lin.heroic_spirit_spell.mixin;

import com.lin.heroic_spirit_spell.util.HolyFortifyOverflow;
import io.redspace.ironsspellbooks.entity.spells.wisp.WispEntity;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.UUID;
import java.lang.reflect.Field;

@Mixin(Entity.class)
public abstract class WispRemovalMixin {

    @Inject(method = "remove", at = @At("HEAD"))
    private void heroicSpiritSpell$onWispRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        if ((Object) this instanceof WispEntity) {
            WispEntity wisp = (WispEntity) (Object) this;
            Level level = wisp.level();
            // 只在服务端，且是因为被丢弃(撞击)或被杀(超时/受击)时触发
            if (!level.isClientSide
                    && (reason == Entity.RemovalReason.DISCARDED || reason == Entity.RemovalReason.KILLED)) {

                // 获取 cachedOwner
                Entity cachedOwner = null;
                try {
                    // 由于 cachedOwner 是 private 的，我们需要反射获取，或者通过 WispEntity 的公有方法获取
                    // WispEntity 有 getOwner() 方法吗？根据 javap，没有 public getOwner()。
                    // 但是它有 setOwner(Entity)。
                    // 我们可以使用 Mixin 的 @Shadow 在 WispEntityMixin 中，但这无法在这里访问。
                    // 幸好，WispEntity 继承自 PathfinderMob -> Mob -> LivingEntity -> Entity
                    // 我们可以尝试通过 Projectile 的 getOwner 吗？WispEntity 不是 Projectile。
                    // 反射是最后的手段。
                    // 让我们再次检查 javap 输出。
                    // private net.minecraft.world.entity.Entity cachedOwner;

                    Field ownerField = WispEntity.class.getDeclaredField("cachedOwner");
                    ownerField.setAccessible(true);
                    cachedOwner = (Entity) ownerField.get(wisp);
                } catch (Exception e) {
                    // 忽略反射错误
                }

                // 范围效果：半径 5 格
                AABB area = wisp.getBoundingBox().inflate(5.0);
                List<LivingEntity> nearbyEntities = level.getEntitiesOfClass(LivingEntity.class, area);

                for (LivingEntity entity : nearbyEntities) {
                    if (entity == wisp)
                        continue;

                    boolean isAlly = (cachedOwner != null && entity.isAlliedTo(cachedOwner));

                    if (isAlly) {
                        // 同队：幸运 V (Amplifier 4) 持续 1s (20 ticks)
                        entity.addEffect(new MobEffectInstance(MobEffects.LUCK, 20, 4));

                        // 恢复 10 生命并转化溢出为护盾
                        float healAmount = 10.0f;
                        float currentHealth = entity.getHealth();
                        float maxHealth = entity.getMaxHealth();
                        float missingHealth = maxHealth - currentHealth;
                        entity.heal(healAmount);

                        HolyFortifyOverflow.applyOverflowFortify(entity, healAmount - missingHealth);
                    } else {
                        // 非同队：霉运 V (Amplifier 4) 持续 1s (20 ticks)
                        entity.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 20, 4));
                    }
                }

                // 粒子特效
                if (level instanceof ServerLevel serverLevel) {
                    // 1. Flash 粒子
                    serverLevel.sendParticles(ParticleTypes.FLASH, wisp.getX(), wisp.getY(), wisp.getZ(), 1, 0, 0, 0,
                            0);

                    // 2. Totem of Undying 粒子圈 (半径 5)
                    double radius = 5.0;
                    int particleCount = 60;
                    for (int i = 0; i < particleCount; i++) {
                        double angle = 2 * Math.PI * i / particleCount;
                        double x = wisp.getX() + radius * Math.cos(angle);
                        double z = wisp.getZ() + radius * Math.sin(angle);
                        serverLevel.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, x, wisp.getY(), z, 1, 0, 0, 0, 0);
                    }

                    // 3. 大量 Wisp 粒子
                    serverLevel.sendParticles(ParticleHelper.WISP, wisp.getX(), wisp.getY() + 0.5, wisp.getZ(), 50, 2.0,
                            2.0, 2.0, 0.1);
                }
            }
        }
    }
}
