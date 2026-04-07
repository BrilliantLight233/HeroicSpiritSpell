package com.lin.heroic_spirit_spell.mixin;

import com.lin.heroic_spirit_spell.util.HolyShieldHelper;
import io.redspace.ironsspellbooks.entity.spells.shield.ShieldEntity;
import io.redspace.ironsspellbooks.entity.spells.shield.ShieldTrimLayer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ShieldTrimLayer.class, remap = false)
public abstract class ShieldTrimLayerMixin {
    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/RenderType;energySwirl(Lnet/minecraft/resources/ResourceLocation;FF)Lnet/minecraft/client/renderer/RenderType;"))
    private RenderType heroicSpiritSpell$swapHolyShieldTrim(ResourceLocation texture, float xOffset, float yOffset, net.minecraft.client.renderer.MultiBufferSource bufferSource, int packedLight, ShieldEntity entity, float pLimbSwing, float pLimbSwingAmount, float partialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
        if (HolyShieldHelper.isHolyShield(entity)) {
            texture = HolyShieldHelper.getShieldTextures(entity).trim();
        }
        return RenderType.energySwirl(texture, xOffset, yOffset);
    }
}
