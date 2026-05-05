package com.lin.heroic_spirit_spell.mixin;

import com.lin.heroic_spirit_spell.util.FireShieldHelper;
import com.lin.heroic_spirit_spell.util.HolyShieldHelper;
import io.redspace.ironsspellbooks.entity.spells.AbstractShieldEntity;
import io.redspace.ironsspellbooks.entity.spells.shield.ShieldEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ShieldEntity.class, remap = false)
public abstract class ShieldEntityMixin {
    @Inject(method = "takeDamage", at = @At("HEAD"), cancellable = true)
    private void heroicSpiritSpell$ignoreAlliedDamage(DamageSource source, float amount, @Nullable Vec3 location, CallbackInfo ci) {
        AbstractShieldEntity shieldEntity = (AbstractShieldEntity) (Object) this;
        ServerPlayer shieldOwner = HolyShieldHelper.getShieldOwner(shieldEntity);
        Entity attacker = HolyShieldHelper.pickDamageAttacker(source, shieldOwner);
        if (HolyShieldHelper.isHolyShield(shieldEntity) && HolyShieldHelper.shouldIgnoreDamage(shieldEntity, attacker)) {
            ci.cancel();
            return;
        }
        if (FireShieldHelper.isFireShield(shieldEntity) && FireShieldHelper.shouldIgnoreDamage(shieldEntity, attacker)) {
            ci.cancel();
        }
    }

    @Inject(method = "takeDamage", at = @At("TAIL"))
    private void heroicSpiritSpell$trackHolyShieldDamage(DamageSource source, float amount, @Nullable Vec3 location, CallbackInfo ci) {
        AbstractShieldEntity shieldEntity = (AbstractShieldEntity) (Object) this;
        if (HolyShieldHelper.isHolyShield(shieldEntity) && !shieldEntity.level().isClientSide) {
            HolyShieldHelper.afterShieldDamaged(shieldEntity);
            return;
        }
        if (FireShieldHelper.isFireShield(shieldEntity) && !shieldEntity.level().isClientSide) {
            FireShieldHelper.afterShieldDamaged(shieldEntity);
        }
    }

    @Inject(method = "destroy", at = @At("HEAD"), cancellable = true)
    private void heroicSpiritSpell$suppressDefaultBreakSound(CallbackInfo ci) {
        AbstractShieldEntity shieldEntity = (AbstractShieldEntity) (Object) this;
        if (HolyShieldHelper.isHolyShield(shieldEntity) || FireShieldHelper.isFireShield(shieldEntity)) {
            shieldEntity.discard();
            ci.cancel();
        }
    }
}
