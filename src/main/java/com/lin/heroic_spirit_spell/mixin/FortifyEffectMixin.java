package com.lin.heroic_spirit_spell.mixin;

import com.lin.heroic_spirit_spell.util.HolyFortifyLogic;
import io.redspace.ironsspellbooks.effect.FortifyEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 全局修正神圣守护：按当前黄心换算等价档位，吸收增量为 (amplifier - baseEquiv)，无 20 级上限。
 * 若本次施加的 Amplifier 低于当前黄心等价档位，则不压低黄心（低等级效果不覆盖更高黄心）。
 */
@Mixin(value = FortifyEffect.class, remap = false)
public class FortifyEffectMixin {

    @Redirect(
            method = "onEffectStarted",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;setAbsorptionAmount(F)V"),
            remap = true)
    private void heroicSpiritSpell$redirectFortifyAbsorption(LivingEntity entity, float wrongValue) {
        float absBefore = entity.getAbsorptionAmount();
        float delta = wrongValue - absBefore;
        int amplifier = Math.round(delta - 1.0f);
        int baseEquiv = HolyFortifyLogic.equivalentAmplifierFromAbsorption(absBefore);
        float corrected = HolyFortifyLogic.correctedAbsorption(absBefore, amplifier, baseEquiv);
        if (corrected < absBefore) {
            corrected = absBefore;
        }
        entity.setAbsorptionAmount(corrected);
    }
}
