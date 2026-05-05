package com.lin.heroic_spirit_spell.effect;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * ��������������ÿ��Ϊ {@link AttributeRegistry#CAST_TIME_REDUCTION} ���� +0.05������ħ���ٷֱ�ʩ������һ�£���Чÿ��Լ 5% �����������Σ���
 */
public final class SpellChantReductionEffect extends MagicMobEffect {

    private static final ResourceLocation MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(HeroicSpiritSpell.MODID, "spell_chant_reduction");

    public SpellChantReductionEffect(MobEffectCategory category, int color) {
        super(category, color);
        this.addAttributeModifier(
                AttributeRegistry.CAST_TIME_REDUCTION,
                MODIFIER_ID,
                0.05,
                AttributeModifier.Operation.ADD_VALUE);
    }
}
