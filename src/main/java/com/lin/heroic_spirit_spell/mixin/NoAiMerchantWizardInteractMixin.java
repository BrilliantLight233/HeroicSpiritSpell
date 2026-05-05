package com.lin.heroic_spirit_spell.mixin;

import io.redspace.ironsspellbooks.entity.mobs.wizards.alchemist.ApothecaristEntity;
import io.redspace.ironsspellbooks.entity.mobs.wizards.cryomancer.CryomancerEntity;
import io.redspace.ironsspellbooks.entity.mobs.wizards.priest.PriestEntity;
import io.redspace.ironsspellbooks.entity.mobs.wizards.pyromancer.PyromancerEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({PyromancerEntity.class, CryomancerEntity.class, PriestEntity.class, ApothecaristEntity.class})
public abstract class NoAiMerchantWizardInteractMixin {
    @Inject(method = "mobInteract", at = @At("HEAD"), cancellable = true)
    private void heroicSpiritSpell$blockTradeWhenNoAi(Player player, InteractionHand hand,
                                                      CallbackInfoReturnable<InteractionResult> cir) {
        if (((Mob) (Object) this).isNoAi()) {
            cir.setReturnValue(InteractionResult.PASS);
        }
    }
}
