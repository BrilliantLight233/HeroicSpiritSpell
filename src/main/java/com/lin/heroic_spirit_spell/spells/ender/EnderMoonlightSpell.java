package com.lin.heroic_spirit_spell.spells.ender;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.entity.spells.EnderMoonlightChargeCutEntity;
import com.lin.heroic_spirit_spell.registry.ModEntities;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.particle.TraceParticleOptions;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;

/**
 * ????????????????????????????????????????? ender_slash ???????????????? 16 ???????????????
 */
public class EnderMoonlightSpell extends AbstractSpell {

    private static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(HeroicSpiritSpell.MODID, "ender_moonlight");

    /** apprentice_codex moon_light: sounds.json key moon_light_dimension */
    private static final ResourceLocation MOON_LIGHT_DIMENSION_SOUND_ID =
            ResourceLocation.fromNamespaceAndPath("apprenticecodex", "moon_light_dimension");

    /** ShadowSlash trace tint */
    private static final Vector3f SHADOW_TRACE_COLOR = new Vector3f(1f, 0.333f, 1f);

    private static final float SHADOW_DASH_RAY_DISTANCE = 12f;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(4)
            .setCooldownSeconds(0)
            .build();

    public EnderMoonlightSpell() {
        this.baseSpellPower = 10;
        this.spellPowerPerLevel = 10;
        this.castTime = 0;
        this.baseManaCost = 500;
        this.manaCostPerLevel = 0;
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
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float spellPower = getSpellPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(spellPower, 2)),
                Component.translatable("spell.heroic_spirit_spell.ender_moonlight.hp_scaling")
        );
    }

    @Override
    public void onClientCast(Level level, int spellLevel, LivingEntity entity, ICastData castData) {
        super.onClientCast(level, spellLevel, entity, castData);
        entity.setYBodyRot(entity.getYRot());
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return BuiltInRegistries.SOUND_EVENT
                .getOptional(MOON_LIGHT_DIMENSION_SOUND_ID)
                .or(() -> Optional.of(SoundRegistry.SHADOW_SLASH.get()));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ONE_HANDED_VERTICAL_UPSWING_ANIMATION;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level.isClientSide) {
            return;
        }

        // 1) ????????????? Trace ??????????????? EnderSlashParticleOptions??
        Vec3 forward = entity.getForward();
        Vec3 end = Utils.raycastForBlock(level, entity.getEyePosition(),
                entity.getEyePosition().add(forward.scale(SHADOW_DASH_RAY_DISTANCE)), ClipContext.Fluid.NONE).getLocation();
        Vec3 rayVector = end.subtract(entity.getEyePosition());
        Vec3 impulse = rayVector.scale(1 / 6f).add(0, 0.1, 0);
        entity.setDeltaMovement(entity.getDeltaMovement().scale(0.2).add(impulse));
        entity.hurtMarked = true;
        entity.addEffect(new MobEffectInstance(MobEffectRegistry.FALL_DAMAGE_IMMUNITY, 20, 0, false, false, true));

        int trailParticles = 15;
        double speed = rayVector.length() / 12.0 * .75;
        for (int i = 0; i < trailParticles; i++) {
            Vec3 particleStart = entity.getBoundingBox().getCenter().add(Utils.getRandomVec3(1 + entity.getBbWidth()));
            Vec3 particleEnd = particleStart.add(rayVector);
            MagicManager.spawnParticles(level,
                    new TraceParticleOptions(Utils.v3f(particleEnd), SHADOW_TRACE_COLOR),
                    particleStart.x, particleStart.y, particleStart.z, 1, 0, 0, 0, speed, false);
        }

        // 2) ?????????? apprentice_codex MoonLightChargeCut???????? 16
        Vec3 dir = entity.getLookAngle();
        if (dir.lengthSqr() < 1e-6) {
            dir = Vec3.directionFromRotation(entity.getXRot(), entity.getYRot());
        }
        dir = dir.normalize();
        Vec3 startPos = entity.position().add(dir.scale(
                EnderMoonlightChargeCutEntity.START_OFFSET_BLOCKS + EnderMoonlightChargeCutEntity.SURFACE_OFFSET_BLOCKS));
        float baseDamage = getSpellPower(spellLevel, entity);
        var cut = new EnderMoonlightChargeCutEntity(ModEntities.ENDER_MOONLIGHT_CHARGE_CUT.get(), level, entity);
        cut.setPos(startPos.x, startPos.y, startPos.z);
        cut.setYRot(entity.getYRot());
        cut.setXRot(entity.getXRot());
        cut.setup(EnderMoonlightChargeCutEntity.FIXED_DISTANCE_BLOCKS, baseDamage);
        level.addFreshEntity(cut);

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
