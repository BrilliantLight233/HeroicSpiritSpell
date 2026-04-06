package com.lin.heroic_spirit_spell.spells.holy;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.util.HolyShieldHelper;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.CastResult;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.entity.spells.shield.ShieldEntity;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class HolyShieldSpell extends HoldCastSpell {
    private static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(HeroicSpiritSpell.MODID, "holy_shield");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(0.25f)
            .build();

    public HolyShieldSpell() {
        super(20 * 60 * 60);
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
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.HOLY_CAST.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(Component.translatable("ui.irons_spellbooks.hp", HolyShieldHelper.fromCenti(HolyShieldHelper.getMaxHpCenti(spellLevel))));
    }

    @Override
    public CastResult canBeCastedBy(int spellLevel, CastSource castSource, MagicData playerMagicData, Player player) {
        CastResult result = super.canBeCastedBy(spellLevel, castSource, playerMagicData, player);
        if (!result.isSuccess()) {
            return result;
        }
        if (!HolyShieldHelper.hasShieldItem(player)) {
            return new CastResult(CastResult.Type.FAILURE,
                    Component.translatable("spell.heroic_spirit_spell.holy_shield.requires_shield").withStyle(ChatFormatting.RED));
        }
        if (HolyShieldHelper.getBreakTicks(player) > 0) {
            float seconds = HolyShieldHelper.getBreakTicks(player) / 20.0f;
            return new CastResult(CastResult.Type.FAILURE,
                    Component.translatable("spell.heroic_spirit_spell.holy_shield.disabled", String.format(Locale.ROOT, "%.1f", seconds)).withStyle(ChatFormatting.RED));
        }
        return result;
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        return entity instanceof ServerPlayer serverPlayer && HolyShieldHelper.hasShieldItem(serverPlayer);
    }

    @Override
    public void onServerPreCast(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        super.onServerPreCast(level, spellLevel, entity, playerMagicData);
        if (!(entity instanceof ServerPlayer serverPlayer)) {
            return;
        }
        HolyShieldHelper.ensureCapacityForLevel(serverPlayer, spellLevel);
        ShieldEntity shieldEntity = HolyShieldHelper.getOrCreateShield(serverPlayer, spellLevel);
        HolyShieldHelper.updateShieldAnchor(shieldEntity, serverPlayer);
        HolyShieldHelper.syncShieldHealth(serverPlayer, shieldEntity);
        HolyShieldHelper.syncScoreboard(serverPlayer);
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (!(entity instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!HolyShieldHelper.hasShieldItem(serverPlayer)) {
            HolyShieldHelper.clearShield(serverPlayer);
            cancelHold(serverPlayer, false);
            return;
        }
        if (HolyShieldHelper.getBreakTicks(serverPlayer) > 0) {
            HolyShieldHelper.clearShield(serverPlayer);
            cancelHold(serverPlayer, false);
            return;
        }
        ShieldEntity shieldEntity = HolyShieldHelper.getOrCreateShield(serverPlayer, spellLevel);
        HolyShieldHelper.updateShieldAnchor(shieldEntity, serverPlayer);
        HolyShieldHelper.syncShieldHealth(serverPlayer, shieldEntity);
        HolyShieldHelper.syncScoreboard(serverPlayer);
        HolyShieldHelper.sendHud(serverPlayer);
        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled) {
        if (entity instanceof ServerPlayer serverPlayer) {
            HolyShieldHelper.clearShield(serverPlayer);
            HolyShieldHelper.syncScoreboard(serverPlayer);
        }
        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }
}
