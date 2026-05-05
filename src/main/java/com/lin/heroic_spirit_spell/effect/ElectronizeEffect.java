package com.lin.heroic_spirit_spell.effect;

import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Thunderstruck phase-1 state effect. Behavior is handled in ThunderstruckRuntime/Events. */
public final class ElectronizeEffect extends MagicMobEffect {
    public ElectronizeEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
