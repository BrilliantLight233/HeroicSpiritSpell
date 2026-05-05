package com.lin.heroic_spirit_spell.mixin;

import io.redspace.ironsspellbooks.spells.lightning.ChargeSpell;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = ChargeSpell.class, remap = false)
public abstract class ChargeSpellTooltipMixin {
    private static final float HEROIC_SPIRIT_SPELL$PER_LEVEL_PERCENT = 0.05f;

    @ModifyConstant(method = "getPercentAttackDamage", constant = @Constant(floatValue = 0.1f))
    private float heroicSpiritSpell$setChargedAttackPerLevel(float original) {
        return HEROIC_SPIRIT_SPELL$PER_LEVEL_PERCENT;
    }

    @ModifyConstant(method = "getPercentSpeed", constant = @Constant(floatValue = 0.2f))
    private float heroicSpiritSpell$setChargedSpeedPerLevel(float original) {
        return HEROIC_SPIRIT_SPELL$PER_LEVEL_PERCENT;
    }
}
