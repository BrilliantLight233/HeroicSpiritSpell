package com.lin.heroic_spirit_spell.spells.ender;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.network.GravityCageRingYOffsetPayload;
import com.lin.heroic_spirit_spell.util.GravityCageRuntime;
import com.lin.heroic_spirit_spell.util.TargetAreaVisualOffset;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.spells.target_area.TargetedAreaEntity;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * Purple target_area follows caster for 3s; runtime handles edge push and particles.
 */
public class GravityCageSpell extends AbstractSpell {

    private static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(HeroicSpiritSpell.MODID, "gravity_cage");

    private static final int DURATION_TICKS = 60;
    private static final Vector3f PREVIEW_COLOR = new Vector3f(0.58f, 0.18f, 0.85f);

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(12)
            .build();

    public GravityCageSpell() {
        this.castTime = 0;
        this.baseManaCost = 0;
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 0;
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
    public Vector3f getTargetingColor() {
        return PREVIEW_COLOR;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("spell.heroic_spirit_spell.gravity_cage.info_radius", (int) GravityCageRuntime.RADIUS),
                Component.translatable("spell.heroic_spirit_spell.gravity_cage.info_duration", Utils.timeFromTicks(DURATION_TICKS * 1.0f, 1))
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.ENDER_CAST.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.SELF_CAST_ANIMATION;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData magicData) {
        if (level.isClientSide || !(level instanceof ServerLevel sl)) {
            super.onCast(level, spellLevel, entity, castSource, magicData);
            return;
        }
        int color = Utils.packRGB(PREVIEW_COLOR);
        Vec3 spawn = entity.position().add(0.0, 1.0, 0.0);
        TargetedAreaEntity area = TargetedAreaEntity.createTargetAreaEntity(
                sl, spawn, GravityCageRuntime.RADIUS, color, entity);
        area.setDuration(DURATION_TICKS);
        area.setShouldFade(true);
        TargetAreaVisualOffset.track(area.getUUID());
        PacketDistributor.sendToPlayersTrackingEntity(area, new GravityCageRingYOffsetPayload(area.getUUID()));
        GravityCageRuntime.start(sl, entity, area.getId(), DURATION_TICKS);
        super.onCast(level, spellLevel, entity, castSource, magicData);
    }
}
