package com.lin.heroic_spirit_spell.mixin;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.spells.holy.HealSpell;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Irons Heal: after cast, grant Player Luck VI for 1s. */
@Mixin(HealSpell.class)
public abstract class HealSpellMixin {

    private static final int LUCK_VI_AMPLIFIER = 5;
    private static final int ONE_SECOND_TICKS = 20;

    @Inject(method = "onCast", at = @At("TAIL"), remap = false)
    private void heroicSpiritSpell$luckOnHeal(
            Level world,
            int spellLevel,
            LivingEntity entity,
            CastSource castSource,
            MagicData playerMagicData,
            CallbackInfo ci) {
        if (world.isClientSide || !(entity instanceof Player)) {
            return;
        }
        entity.addEffect(new MobEffectInstance(
                MobEffects.LUCK,
                ONE_SECOND_TICKS,
                LUCK_VI_AMPLIFIER,
                false,
                true,
                true));
    }
}
