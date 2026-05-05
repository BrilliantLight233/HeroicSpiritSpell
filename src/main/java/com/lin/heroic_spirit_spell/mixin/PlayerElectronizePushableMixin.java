package com.lin.heroic_spirit_spell.mixin;

import com.lin.heroic_spirit_spell.registry.ModEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class PlayerElectronizePushableMixin {
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void heroicSpiritSpell$notPushableDuringElectronize(CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player player && player.hasEffect(ModEffects.ELECTRONIZE)) {
            cir.setReturnValue(false);
        }
    }
}
