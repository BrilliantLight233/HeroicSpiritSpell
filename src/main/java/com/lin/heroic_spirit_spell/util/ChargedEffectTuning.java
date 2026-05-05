package com.lin.heroic_spirit_spell.util;

import io.redspace.ironsspellbooks.IronsSpellbooks;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

public final class ChargedEffectTuning {
    private static final double HEROIC_SPIRIT_SPELL$PER_LEVEL_MULTIPLIER = 0.05d;

    private ChargedEffectTuning() {
    }

    public static void apply() {
        MobEffectRegistry.CHARGED.get()
                .addAttributeModifier(
                        Attributes.ATTACK_DAMAGE,
                        IronsSpellbooks.id("mobeffect_charged"),
                        HEROIC_SPIRIT_SPELL$PER_LEVEL_MULTIPLIER,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                .addAttributeModifier(
                        Attributes.MOVEMENT_SPEED,
                        IronsSpellbooks.id("mobeffect_charged"),
                        HEROIC_SPIRIT_SPELL$PER_LEVEL_MULTIPLIER,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL)
                .addAttributeModifier(
                        AttributeRegistry.SPELL_POWER,
                        IronsSpellbooks.id("mobeffect_charged"),
                        HEROIC_SPIRIT_SPELL$PER_LEVEL_MULTIPLIER,
                        AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }
}
