package com.lin.heroic_spirit_spell.util;

import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;

/**
 * 圣灵 / 净化：溢出治疗转神圣守护。最高叠至 20 级（Amplifier 19）黄心；
 * 若当前黄心已等价于 21 级及以上（{@code baseEquiv >= 20}），则不施加、不刷新、不改变吸收。
 * 具体吸收修正由 {@link com.lin.heroic_spirit_spell.mixin.FortifyEffectMixin} 全局处理。
 */
public final class HolyFortifyOverflow {

    private static final int FORTIFY_DURATION_TICKS = 20 * 10;
    /** 20 级神圣守护 → Amplifier 19 */
    private static final int WISP_CLEANSE_MAX_AMPLIFIER = 19;
    /** 已有 21 级及以上等价黄心则圣灵/净化不干预 */
    private static final int WISP_CLEANSE_SKIP_IF_BASE_EQUIV_GTE = 20;

    private HolyFortifyOverflow() {
    }

    public static void applyOverflowFortify(LivingEntity target, float overflow) {
        if (target.level().isClientSide() || overflow < 1.0f) {
            return;
        }

        int overflowAmp = (int) (overflow / 2);
        if (overflowAmp < 0) {
            overflowAmp = 0;
        }

        MobEffectInstance currentEffect = target.getEffect(MobEffectRegistry.FORTIFY);
        float absorptionBefore = target.getAbsorptionAmount();

        int baseEquiv = currentEffect == null ? -1 : HolyFortifyLogic.equivalentAmplifierFromAbsorption(absorptionBefore);
        if (currentEffect != null && baseEquiv >= WISP_CLEANSE_SKIP_IF_BASE_EQUIV_GTE) {
            return;
        }

        int newAmp = Math.max(0, Math.min(WISP_CLEANSE_MAX_AMPLIFIER, baseEquiv + overflowAmp));

        if (currentEffect != null) {
            target.removeEffect(MobEffectRegistry.FORTIFY);
        }
        target.addEffect(new MobEffectInstance(MobEffectRegistry.FORTIFY, FORTIFY_DURATION_TICKS, newAmp));
    }
}
