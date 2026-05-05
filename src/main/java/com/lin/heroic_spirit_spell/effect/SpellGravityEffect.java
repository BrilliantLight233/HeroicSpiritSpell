package com.lin.heroic_spirit_spell.effect;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * +0.1 gravity per effect level (vanilla stacks amplifier like other attribute effects).
 */
public final class SpellGravityEffect extends MagicMobEffect {

    private static final ResourceLocation MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(HeroicSpiritSpell.MODID, "spell_gravity");

    public SpellGravityEffect(MobEffectCategory category, int color) {
        super(category, color);
        this.addAttributeModifier(Attributes.GRAVITY, MODIFIER_ID, 0.1, AttributeModifier.Operation.ADD_VALUE);
    }
}
