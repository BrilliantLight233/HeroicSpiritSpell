package com.lin.heroic_spirit_spell.mixin;

import com.lin.heroic_spirit_spell.util.FireShieldHelper;
import com.lin.heroic_spirit_spell.util.HolyShieldHelper;
import com.mojang.blaze3d.vertex.PoseStack;
import io.redspace.ironsspellbooks.entity.spells.shield.ShieldEntity;
import io.redspace.ironsspellbooks.entity.spells.shield.ShieldRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ShieldRenderer.class, remap = false)
public abstract class ShieldRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void heroicSpiritSpell$hideFireShieldRender(ShieldEntity entity, float entityYaw, float partialTicks,
                                                        PoseStack poseStack, MultiBufferSource bufferSource,
                                                        int packedLight, CallbackInfo ci) {
        if (FireShieldHelper.isFireShield(entity)) {
            ci.cancel();
        }
    }

    @Inject(method = "getTextureLocation", at = @At("HEAD"), cancellable = true)
    private void heroicSpiritSpell$swapHolyShieldOverlay(ShieldEntity entity, CallbackInfoReturnable<ResourceLocation> cir) {
        if (HolyShieldHelper.isHolyShield(entity)) {
            cir.setReturnValue(HolyShieldHelper.getShieldTextures(entity).overlay());
        }
    }
}
