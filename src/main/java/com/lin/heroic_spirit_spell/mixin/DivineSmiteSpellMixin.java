package com.lin.heroic_spirit_spell.mixin;

import io.redspace.ironsspellbooks.spells.holy.DivineSmiteSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 神圣打击：成功造成伤害并执行附魔后效时，为命中目标附加霉运 IV，持续 5 秒。
 */
@Mixin(DivineSmiteSpell.class)
public abstract class DivineSmiteSpellMixin {

    private static final int HEROIC_SPIRIT_SPELL$UNLUCK_DURATION_TICKS = 5 * 20;
    /** 等级 IV → amplifier 3（0 基） */
    private static final int HEROIC_SPIRIT_SPELL$UNLUCK_AMPLIFIER = 3;

    @Redirect(
            method = "onCast",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/enchantment/EnchantmentHelper;doPostAttackEffects(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private static void heroicSpiritSpell$unluckAfterPostAttack(
            ServerLevel level, Entity target, DamageSource damageSource) {
        EnchantmentHelper.doPostAttackEffects(level, target, damageSource);
        if (!level.isClientSide() && target instanceof LivingEntity living) {
            living.addEffect(
                    new MobEffectInstance(
                            MobEffects.UNLUCK,
                            HEROIC_SPIRIT_SPELL$UNLUCK_DURATION_TICKS,
                            HEROIC_SPIRIT_SPELL$UNLUCK_AMPLIFIER));
        }
    }
}
