package com.lin.heroic_spirit_spell.mixin;

import io.redspace.ironsspellbooks.entity.spells.ShieldPart;
import io.redspace.ironsspellbooks.entity.spells.shield.ShieldEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** irons_spellbooks:shield 分段实体不再参与实体碰撞（与 AbstractShieldEntity 主体一致）。 */
@Mixin(value = ShieldPart.class, remap = false)
public abstract class ShieldPartMixin {
    @Shadow(remap = false) public io.redspace.ironsspellbooks.entity.spells.AbstractShieldEntity parentEntity;

    @Inject(method = "canBeCollidedWith", at = @At("HEAD"), cancellable = true)
    private void heroicSpiritSpell$shieldPartNoEntityCollision(CallbackInfoReturnable<Boolean> cir) {
        if (parentEntity instanceof ShieldEntity) {
            cir.setReturnValue(false);
        }
    }
}
