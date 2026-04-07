package com.lin.heroic_spirit_spell.mixin;

import com.lin.heroic_spirit_spell.util.HolyShieldHelper;
import io.redspace.ironsspellbooks.entity.spells.ShieldPart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Projectile.class)
public abstract class ProjectileMixin {
    @Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
    private void heroicSpiritSpell$allowAlliedProjectilesThroughHolyShield(Entity target, CallbackInfoReturnable<Boolean> cir) {
        Projectile projectile = (Projectile) (Object) this;
        if (target instanceof ShieldPart shieldPart
                && HolyShieldHelper.isHolyShield(shieldPart.parentEntity)
                && HolyShieldHelper.shouldIgnoreDamage(shieldPart.parentEntity, projectile)) {
            cir.setReturnValue(false);
        }
    }
}
