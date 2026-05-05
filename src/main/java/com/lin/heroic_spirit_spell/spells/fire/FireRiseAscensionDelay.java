package com.lin.heroic_spirit_spell.spells.fire;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;

/**
 * Fire Rise: after burning_dash dash window, apply Slow Falling for 6s (120 ticks).
 */
public final class FireRiseAscensionDelay {

    public static final String PERSIST_KEY = HeroicSpiritSpell.MODID + ":fire_rise_ascension_delay";
    private static final int SLOW_FALLING_DURATION_TICKS = 6 * 20;

    private FireRiseAscensionDelay() {
    }

    public static void tick(LivingEntity entity) {
        if (!entity.getPersistentData().contains(PERSIST_KEY)) {
            return;
        }
        int t = entity.getPersistentData().getInt(PERSIST_KEY) - 1;
        if (t <= 0) {
            entity.getPersistentData().remove(PERSIST_KEY);
            entity.addEffect(new MobEffectInstance(
                    MobEffects.SLOW_FALLING,
                    SLOW_FALLING_DURATION_TICKS,
                    0,
                    false,
                    false,
                    true));
        } else {
            entity.getPersistentData().putInt(PERSIST_KEY, t);
        }
    }
}
