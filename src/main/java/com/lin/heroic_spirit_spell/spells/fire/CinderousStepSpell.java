package com.lin.heroic_spirit_spell.spells.fire;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.registry.ModEffects;
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
import io.redspace.ironsspellbooks.player.ClientInputEvents;
import io.redspace.ironsspellbooks.player.ClientSpellCastHelper;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nullable;

/**
 * Cinderous Step: dash uses same impulse as Irons ShadowSlashSpell (raycast + setDeltaMovement), 5-block ray.
 */
public class CinderousStepSpell extends AbstractSpell {

    private static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(HeroicSpiritSpell.MODID, "cinderous_step");

    /** Eye ray along getForward(), max blocks (ShadowSlash uses 12). */
    private static final float DASH_RAY_DISTANCE = 5.0f;
    /** Irons AbstractSpell.playSound uses volume 2.0; half -> 1.0 */
    private static final float CAST_SOUND_VOLUME = 1.0f;
    /** Chant buff: 2s */
    private static final int CHANT_BUFF_DURATION_TICKS = 40;
    /** Rank 10: amp 9 -> (9+1)*0.05 on cast_time_reduction */
    private static final int CHANT_BUFF_AMPLIFIER = 9;
    private static final int FALL_IMMUNITY_TICKS = 15;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(3.5f)
            .build();

    public CinderousStepSpell() {
        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
        this.baseManaCost = 0;
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
        return List.of(
                Component.translatable("spell.heroic_spirit_spell.cinderous_step.info_distance", Utils.stringTruncation(DASH_RAY_DISTANCE, 0)),
                Component.translatable("spell.heroic_spirit_spell.cinderous_step.info_chant_buff")
        );
    }

    @Override
    public void onClientCast(Level level, int spellLevel, LivingEntity entity, ICastData castData) {
        super.onClientCast(level, spellLevel, entity, castData);
        entity.setYBodyRot(entity.getYRot());
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.FIRE_CAST.get());
    }

    @Override
    public void onClientPreCast(Level level, int spellLevel, LivingEntity entity, InteractionHand hand, @Nullable MagicData playerMagicData) {
        if (getCastType().immediatelySuppressRightClicks()) {
            if (ClientInputEvents.isUseKeyDown()) {
                ClientSpellCastHelper.setSuppressRightClicks(true);
            }
        }
        getCastStartSound().ifPresent(sound ->
                entity.playSound(sound, CAST_SOUND_VOLUME, 0.9f + Utils.random.nextFloat() * 0.2f));
    }

    @Override
    public void onServerPreCast(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        getCastStartSound().ifPresent(sound ->
                entity.playSound(sound, CAST_SOUND_VOLUME, 0.9f + Utils.random.nextFloat() * 0.2f));
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.TOUCH_GROUND_ANIMATION;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level.isClientSide) {
            return;
        }

        // ShadowSlashSpell.onCast: raycast end, velocity impulse (not Entity#move)
        Vec3 forward = entity.getForward();
        Vec3 eye = entity.getEyePosition();
        Vec3 end = Utils.raycastForBlock(
                level,
                eye,
                eye.add(forward.scale(DASH_RAY_DISTANCE)),
                ClipContext.Fluid.NONE
        ).getLocation();
        Vec3 rayVector = end.subtract(eye);
        Vec3 impulse = rayVector.scale(1 / 6f).add(0, 0.1, 0);
        entity.setDeltaMovement(entity.getDeltaMovement().scale(0.2).add(impulse));
        entity.hurtMarked = true;

        Vec3 feet = entity.position();
        Vec3 horizDelta = new Vec3(end.x - feet.x, 0.0, end.z - feet.z);
        double hLen = horizDelta.length();
        int steps = Math.max(1, Mth.ceil(hLen * 2.5));
        for (int i = 0; i <= steps; i++) {
            Vec3 p = hLen < 1e-4 ? feet : feet.add(horizDelta.scale((double) i / steps));
            MagicManager.spawnParticles(
                    level,
                    ParticleHelper.FIERY_SMOKE,
                    p.x,
                    p.y + 0.05,
                    p.z,
                    1,
                    0.12,
                    0.1,
                    0.12,
                    0.02,
                    false);
        }

        entity.addEffect(new MobEffectInstance(
                ModEffects.SPELL_CHANT_REDUCTION,
                CHANT_BUFF_DURATION_TICKS,
                CHANT_BUFF_AMPLIFIER,
                false,
                false,
                false));
        entity.addEffect(new MobEffectInstance(MobEffectRegistry.FALL_DAMAGE_IMMUNITY, FALL_IMMUNITY_TICKS, 0, false, false, true));

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
