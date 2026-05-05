package com.lin.heroic_spirit_spell.mixin.client;

import com.lin.heroic_spirit_spell.registry.ModEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityGlowSuppressionMixin {
    @Inject(method = "isCurrentlyGlowing", at = @At("HEAD"), cancellable = true)
    private void heroicSpiritSpell$hideGlowWhenElectronized(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof LivingEntity living && living.hasEffect(ModEffects.ELECTRONIZE)) {
            cir.setReturnValue(false);
        }
    }
}
