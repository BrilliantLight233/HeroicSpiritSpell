package com.lin.heroic_spirit_spell.spells.lightning;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.util.ThunderstruckRuntime;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public class ThunderstruckSpell extends AbstractSpell {
    private static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(HeroicSpiritSpell.MODID, "thunderstruck");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.LIGHTNING_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(0)
            .build();

    public ThunderstruckSpell() {
        this.castTime = 0;
        this.baseManaCost = 250;
        this.manaCostPerLevel = 0;
        // Non-zero base is required so AbstractSpell#getSpellPower can reflect attribute scaling.
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 0;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return SPELL_ID;
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level.isClientSide) {
            return;
        }
        if (!(entity instanceof ServerPlayer player)) {
            return;
        }

        if (ThunderstruckRuntime.isStageOneActive(player)) {
            ThunderstruckRuntime.triggerStageTwo(player);
            super.onCast(level, spellLevel, entity, castSource, playerMagicData);
            return;
        }

        ThunderstruckRuntime.startStageOne(player);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
