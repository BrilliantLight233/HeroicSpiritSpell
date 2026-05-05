package com.lin.heroic_spirit_spell.mixin;

import com.lin.heroic_spirit_spell.registry.ModEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityElectronizePushMixin {
    @Inject(method = "push(Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void heroicSpiritSpell$disableOutgoingPushDuringElectronize(Entity other, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        boolean selfIsElectronizedPlayer = self instanceof Player player
                && player.hasEffect(ModEffects.ELECTRONIZE);
        boolean otherIsElectronizedPlayer = other instanceof Player player
                && player.hasEffect(ModEffects.ELECTRONIZE);
        boolean involvesLivingEntity = self instanceof LivingEntity || other instanceof LivingEntity;
        if ((selfIsElectronizedPlayer || otherIsElectronizedPlayer) && involvesLivingEntity) {
            ci.cancel();
        }
    }
}
