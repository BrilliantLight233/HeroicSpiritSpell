package com.lin.heroic_spirit_spell.mixin.client;



import com.lin.heroic_spirit_spell.client.DualShieldAnimationHelper;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.player.AbstractClientPlayer;

import net.minecraft.client.player.LocalPlayer;

import net.minecraft.client.renderer.ItemInHandRenderer;

import net.minecraft.client.renderer.MultiBufferSource;

import net.minecraft.world.InteractionHand;

import net.minecraft.world.entity.HumanoidArm;

import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;

import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.Redirect;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;



@Mixin(value = ItemInHandRenderer.class, priority = 500)

public abstract class DualShieldFirstPersonMixin {



    @Inject(

            method =

                    "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",

            at = @At("HEAD"))

    private void heroicSpiritSpell$fpCaptureHand(

            AbstractClientPlayer player,

            float equippedProgress,

            float partialTicks,

            InteractionHand hand,

            float swingProgress,

            ItemStack stack,

            float raised,

            PoseStack poseStack,

            MultiBufferSource buffer,

            int combinedLight,

            CallbackInfo ci) {

        DualShieldAnimationHelper.beginFirstPersonArmRender(player, hand);

    }



    @Inject(

            method =

                    "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",

            at = @At("RETURN"))

    private void heroicSpiritSpell$fpClearHand(

            AbstractClientPlayer player,

            float equippedProgress,

            float partialTicks,

            InteractionHand hand,

            float swingProgress,

            ItemStack stack,

            float raised,

            PoseStack poseStack,

            MultiBufferSource buffer,

            int combinedLight,

            CallbackInfo ci) {

        DualShieldAnimationHelper.endFirstPersonArmRender();

    }



    @Redirect(

            method =

                    "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",

            at =

                    @At(

                            value = "INVOKE",

                            target =

                                    "Lnet/minecraft/client/player/AbstractClientPlayer;getUsedItemHand()Lnet/minecraft/world/InteractionHand;",

                            ordinal = 0))

    private InteractionHand heroicSpiritSpell$fpMirrorUsedHand0(AbstractClientPlayer player) {

        return DualShieldAnimationHelper.adjustUsedItemHandDuringFirstPersonArmRender(player);

    }



    @Redirect(

            method =

                    "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",

            at =

                    @At(

                            value = "INVOKE",

                            target =

                                    "Lnet/minecraft/client/player/AbstractClientPlayer;getUsedItemHand()Lnet/minecraft/world/InteractionHand;",

                            ordinal = 1))

    private InteractionHand heroicSpiritSpell$fpMirrorUsedHand1(AbstractClientPlayer player) {

        return DualShieldAnimationHelper.adjustUsedItemHandDuringFirstPersonArmRender(player);

    }



    @Redirect(

            method =

                    "renderArmWithItem(Lnet/minecraft/client/player/AbstractClientPlayer;FFLnet/minecraft/world/InteractionHand;FLnet/minecraft/world/item/ItemStack;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",

            at =

                    @At(

                            value = "INVOKE",

                            target =

                                    "Lnet/neoforged/neoforge/client/extensions/common/IClientItemExtensions;applyForgeHandTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/entity/HumanoidArm;Lnet/minecraft/world/item/ItemStack;FFF)Z"))

    private boolean heroicSpiritSpell$fpDualShieldLetVanillaPoseInactiveArm(

            IClientItemExtensions extensions,

            PoseStack poseStack,

            LocalPlayer player,

            HumanoidArm arm,

            ItemStack stack,

            float a,

            float b,

            float c) {

        if (DualShieldAnimationHelper.shouldForceVanillaHandTransformInsteadOfForge(player)) {

            return false;

        }

        return extensions.applyForgeHandTransform(poseStack, player, arm, stack, a, b, c);

    }

}


