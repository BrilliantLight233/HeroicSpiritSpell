package com.lin.heroic_spirit_spell.mixin.client;



import com.lin.heroic_spirit_spell.client.DualShieldAnimationHelper;

import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;

import org.spongepowered.asm.mixin.injection.Redirect;



/**

 * Vanilla {@code HumanoidModel.setupAnim} uses a single-arm path while {@link LivingEntity#isUsingItem()} is true,

 * so only the used arm runs {@code poseRightArm}/{@code poseLeftArm}. Spoofing {@code isUsingItem} to false for dual

 * shield block (ordinal matches the arm-pose branch only) restores the dual-arm pose path.

 */

@Mixin(net.minecraft.client.model.HumanoidModel.class)

public abstract class DualShieldHumanoidSetupAnimMixin<T extends LivingEntity> {



    @Redirect(

            method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",

            at =

                    @At(

                            value = "INVOKE",

                            target = "Lnet/minecraft/world/entity/LivingEntity;isUsingItem()Z",

                            ordinal = 0))

    private boolean heroicSpiritSpell$dualShieldSpoofUsingItemForArmPoses(LivingEntity entity) {

        if (entity instanceof Player player && DualShieldAnimationHelper.isDualShieldBlockVisualActive(player)) {

            return false;

        }

        return entity.isUsingItem();

    }

}


