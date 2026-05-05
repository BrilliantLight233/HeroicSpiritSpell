package com.lin.heroic_spirit_spell.mixin.client;

import com.lin.heroic_spirit_spell.registry.ModEffects;
import com.mojang.blaze3d.vertex.PoseStack;
import io.redspace.ironsspellbooks.render.EnergySwirlLayer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = EnergySwirlLayer.Vanilla.class, remap = false)
public abstract class EnergySwirlChargedLayerMixin {
    @Shadow
    @Final
    private ResourceLocation TEXTURE;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void heroicSpiritSpell$hideChargedSwirlWhenElectronized(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            Player player,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo ci
    ) {
        if (player.hasEffect(ModEffects.ELECTRONIZE) && EnergySwirlLayer.CHARGE_TEXTURE.equals(TEXTURE)) {
            ci.cancel();
        }
    }
}
