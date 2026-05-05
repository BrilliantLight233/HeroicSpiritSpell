package com.lin.heroic_spirit_spell.spells.fire;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.util.FireShieldHelper;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Optional;

/** Driven by vanilla shield blocking; see FireShieldEvents. Cannot be cast with the spell key. */
public class FireShieldSpell extends AbstractSpell {
    private static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(HeroicSpiritSpell.MODID, "fire_shield");
    private static final ResourceLocation SHIELD_SOUND = ResourceLocation.parse("irons_spellbooks:shield");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(0.25f)
            .build();

    public FireShieldSpell() {
        this.castTime = 0;
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 10;
        this.baseManaCost = 0;
        this.manaCostPerLevel = 0;
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
    public CastType getCastType() {
        return CastType.INSTANT;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getOptional(SHIELD_SOUND);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return net.minecraft.core.registries.BuiltInRegistries.SOUND_EVENT.getOptional(SHIELD_SOUND);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.irons_spellbooks.hp", FireShieldHelper.fromCenti(FireShieldHelper.getMaxHpCenti(spellLevel))));
    }

    @Override
    public CastResult canBeCastedBy(int spellLevel, CastSource castSource, MagicData playerMagicData, Player player) {
        return new CastResult(CastResult.Type.FAILURE);
    }
}
