package com.lin.heroic_spirit_spell.mixin;

import com.lin.heroic_spirit_spell.util.HolyShieldHelper;
import io.redspace.ironsspellbooks.entity.spells.shield.ShieldEntity;
import io.redspace.ironsspellbooks.entity.spells.shield.ShieldRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ShieldRenderer.class, remap = false)
public abstract class ShieldTrimLayerMixin {

    @Inject(
            method = "getTextureLocation(Lio/redspace/ironsspellbooks/entity/spells/shield/ShieldEntity;)Lnet/minecraft/resources/ResourceLocation;",
            at = @At("RETURN"))
    private void heroicSpiritSpell$swapHolyShieldTrim(ShieldEntity entity, CallbackInfoReturnable<ResourceLocation> cir) {
        if (HolyShieldHelper.isHolyShield(entity)) {
            cir.setReturnValue( HolyShieldHelper.getShieldTextures(entity).trim());
        }
    }
}
