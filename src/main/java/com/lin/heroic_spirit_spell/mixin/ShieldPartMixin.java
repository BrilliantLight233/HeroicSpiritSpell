package com.lin.heroic_spirit_spell.mixin;

import com.lin.heroic_spirit_spell.util.HolyShieldHelper;
import io.redspace.ironsspellbooks.entity.spells.AbstractShieldEntity;
import io.redspace.ironsspellbooks.entity.spells.ShieldPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ShieldPart.class, remap = false)
public abstract class ShieldPartMixin {
    @Shadow public AbstractShieldEntity parentEntity;

    @Inject(method = "canBeCollidedWith", at = @At("HEAD"), cancellable = true)
    private void heroicSpiritSpell$holyShieldNoBodyCollision(CallbackInfoReturnable<Boolean> cir) {
        if (HolyShieldHelper.isHolyShield(parentEntity)) {
            cir.setReturnValue(false);
        }
    }
}
