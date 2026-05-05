package com.lin.heroic_spirit_spell.mixin;

import com.lin.heroic_spirit_spell.registry.ModEffects;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerIncorporeityNoPhysicsMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void heroicSpiritSpell$enableNoPhysicsDuringIncorporeity(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (self.hasEffect(ModEffects.INCORPOREITY)) {
            self.noPhysics = true;
            self.fallDistance = 0f;
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void heroicSpiritSpell$restoreNoPhysicsAfterIncorporeity(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        if (!self.hasEffect(ModEffects.INCORPOREITY)
                && !self.isSpectator()) {
            self.noPhysics = false;
        }
    }
}
