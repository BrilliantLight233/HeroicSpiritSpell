package com.lin.heroic_spirit_spell.mixin;

import com.lin.heroic_spirit_spell.ElectrocuteCastHelper;
import io.redspace.ironsspellbooks.spells.lightning.ElectrocuteSpell;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(value = ElectrocuteSpell.class, remap = false)
public class ElectrocuteSpellMixin {

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 100), remap = false)
    private int heroicSpiritSpell$electrocuteCastTimeTicks(int original) {
        return ElectrocuteCastHelper.CAST_TIME_TICKS;
    }
}
