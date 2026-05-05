package com.lin.heroic_spirit_spell.mixin;

import com.lin.heroic_spirit_spell.registry.ModEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityIncorporeityMoveMixin {
    @Inject(method = "move", at = @At("HEAD"))
    private void heroicSpiritSpell$enableNoPhysicsBeforeMove(MoverType moverType, Vec3 movement, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player player && player.hasEffect(ModEffects.INCORPOREITY)) {
            // Use the same no-physics movement branch as spectator-style movement.
            self.noPhysics = true;
        }
    }

    @Inject(method = "move", at = @At("RETURN"))
    private void heroicSpiritSpell$restoreNoPhysicsAfterMove(MoverType moverType, Vec3 movement, CallbackInfo ci) {
        Entity self = (Entity) (Object) this;
        if (self instanceof Player player
                && !player.hasEffect(ModEffects.INCORPOREITY)
                && !player.isSpectator()) {
            self.noPhysics = false;
        }
    }
}
