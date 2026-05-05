package com.lin.heroic_spirit_spell.mixin;

import com.lin.heroic_spirit_spell.util.HolyShieldHelper;
import com.lin.heroic_spirit_spell.util.SummonBlazeRuntime;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.entity.spells.AbstractShieldEntity;
import io.redspace.ironsspellbooks.entity.spells.ShieldPart;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Before Iron's canHitEntity logic: let allied spell projectiles ignore magic shield parts. */
@Mixin(value = AbstractMagicProjectile.class, remap = false)
public abstract class AbstractMagicProjectileMixin {

    @Inject(method = "canHitEntity", at = @At("HEAD"), cancellable = true)
    private void heroicSpiritSpell$canHitEntityHead(Entity pTarget, CallbackInfoReturnable<Boolean> cir) {
        if (pTarget instanceof ShieldPart shieldPart
                && shieldPart.parentEntity instanceof AbstractShieldEntity abstractShield) {
            Projectile self = (Projectile) (Object) this;
            if (HolyShieldHelper.shouldIgnoreDamage(abstractShield, self)) {
                cir.setReturnValue(false);
            }
            return;
        }
        if (pTarget instanceof Blaze blaze) {
            Projectile self = (Projectile) (Object) this;
            Entity owner = self.getOwner();
            if (owner instanceof ServerPlayer sp && SummonBlazeRuntime.isOwnedSummonBlaze(sp, blaze)) {
                cir.setReturnValue(false);
            }
        }
    }
}
