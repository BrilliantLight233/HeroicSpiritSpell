package com.lin.heroic_spirit_spell.effect;

import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/** Standby state; fatal hit handled in ExtremeSenseEvents. */
public final class ExtremeSenseEffect extends MagicMobEffect {

    public ExtremeSenseEffect(MobEffectCategory category, int color) {
        super(category, color);
    }
}
