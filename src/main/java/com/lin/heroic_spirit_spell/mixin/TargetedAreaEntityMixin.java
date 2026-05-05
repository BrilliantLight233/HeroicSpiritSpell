package com.lin.heroic_spirit_spell.mixin;

import com.lin.heroic_spirit_spell.mixin.accessor.EntityLerpAccessor;
import com.lin.heroic_spirit_spell.util.TargetAreaVisualOffset;
import io.redspace.ironsspellbooks.entity.spells.target_area.TargetedAreaEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Gravity cage ring: follow owner with Y+1. Irons copies owner's yo/yOld for interpolation; those must also be shifted
 * or the ring lerps between foot and foot+1 every tick (jitter). Dedicated clients get UUID via network payload to enable the same logic.
 */
@Mixin(value = TargetedAreaEntity.class, remap = false)
public class TargetedAreaEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void heroicSpiritSpell$untrackIfDead(CallbackInfo ci) {
        TargetedAreaEntity self = (TargetedAreaEntity) (Object) this;
        if (TargetAreaVisualOffset.isTracked(self.getUUID()) && (!self.isAlive() || self.isRemoved())) {
            TargetAreaVisualOffset.untrack(self.getUUID());
        }
    }

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/Entity;position()Lnet/minecraft/world/phys/Vec3;",
                    remap = true
            ),
            remap = false
    )
    private Vec3 heroicSpiritSpell$ownerFollowPosWithYOffset(Entity owner) {
        Vec3 p = owner.position();
        TargetedAreaEntity self = (TargetedAreaEntity) (Object) this;
        if (TargetAreaVisualOffset.isTracked(self.getUUID())) {
            return p.add(0.0, 1.0, 0.0);
        }
        return p;
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private void heroicSpiritSpell$matchLerpToYOffset(CallbackInfo ci) {
        TargetedAreaEntity self = (TargetedAreaEntity) (Object) this;
        if (!TargetAreaVisualOffset.isTracked(self.getUUID())) {
            return;
        }
        Entity owner = self.getOwner();
        if (owner == null) {
            return;
        }
        EntityLerpAccessor ring = (EntityLerpAccessor) (Object) self;
        EntityLerpAccessor own = (EntityLerpAccessor) (Object) owner;
        ring.hss_setYo(own.hss_getYo() + 1.0);
        ring.hss_setYOld(own.hss_getYOld() + 1.0);
    }
}
