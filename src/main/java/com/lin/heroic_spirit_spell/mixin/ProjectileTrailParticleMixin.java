package com.lin.heroic_spirit_spell.mixin;

import io.redspace.ironsspellbooks.entity.spells.magic_arrow.MagicArrowProjectile;
import io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin({MagicArrowProjectile.class, MagicMissileProjectile.class})
public abstract class ProjectileTrailParticleMixin {
    @Inject(method = "trailParticles", at = @At("HEAD"), cancellable = true, remap = false)
    private void heroicSpiritSpell$skipEarlyTrailParticles(CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self.level().isClientSide && self.tickCount <= 1) {
            ci.cancel();
        }
    }
}
