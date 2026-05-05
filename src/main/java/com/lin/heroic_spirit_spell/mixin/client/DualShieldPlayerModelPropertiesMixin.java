package com.lin.heroic_spirit_spell.mixin.client;



import com.lin.heroic_spirit_spell.client.DualShieldAnimationHelper;

import net.minecraft.client.model.HumanoidModel;

import net.minecraft.client.model.PlayerModel;

import net.minecraft.client.player.AbstractClientPlayer;

import net.minecraft.client.renderer.entity.player.PlayerRenderer;

import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;

import org.spongepowered.asm.mixin.injection.Inject;

import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;



@Mixin(PlayerRenderer.class)

public abstract class DualShieldPlayerModelPropertiesMixin {



    @Inject(method = "setModelProperties(Lnet/minecraft/client/player/AbstractClientPlayer;)V", at = @At("TAIL"))

    private void heroicSpiritSpell$dualShieldBlockArmPoses(AbstractClientPlayer player, CallbackInfo ci) {

        if (!DualShieldAnimationHelper.isDualShieldBlockVisualActive(player)) {

            return;

        }

        PlayerModel<AbstractClientPlayer> model = ((PlayerRenderer) (Object) this).getModel();

        model.leftArmPose = HumanoidModel.ArmPose.BLOCK;

        model.rightArmPose = HumanoidModel.ArmPose.BLOCK;

    }

}


