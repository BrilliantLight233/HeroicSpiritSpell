package com.lin.heroic_spirit_spell.spells.fire;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
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
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.network.particles.ShockwaveParticlesPacket;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;

/**
 * Detonate: in_fire damage from remaining fire ticks, extinguish; caster gets Regeneration.
 * VFX matches HeatSurge (blastwave, shockwave packet, touch_ground anim).
 */
public class DetonateSpell extends AbstractSpell {

    private static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(HeroicSpiritSpell.MODID, "detonate");

    /** Cubic AABB query; spherical cap via distanceSq */
    private static final float RADIUS = 64f;
    private static final double RADIUS_SQR = RADIUS * RADIUS;
    private static final float DAMAGE_PER_TICK_PER_LEVEL = 0.15f;
    private static final int REGEN_DURATION_TICKS = 5 * 20;
    /** Luck VI, 1s, independent of spell level */
    private static final int LUCK_DURATION_TICKS = 20;
    private static final int LUCK_AMPLIFIER = 5;
    /** Bad Luck III on detonated targets */
    private static final int UNLUCK_DURATION_TICKS = 5 * 20;
    private static final int UNLUCK_AMPLIFIER = 5;

    /** irons_spellbooks entity.generic.fiery_explosion -> fiery_explosion_1.ogg */
    private static final ResourceLocation FIERY_EXPLOSION_SOUND =
            ResourceLocation.parse("irons_spellbooks:entity.generic.fiery_explosion");

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(6)
            .setCooldownSeconds(5f)
            .build();

    public DetonateSpell() {
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
                Component.translatable("spell.heroic_spirit_spell.detonate.info_radius", Utils.stringTruncation(RADIUS, 0)),
                Component.translatable("spell.heroic_spirit_spell.detonate.info_formula")
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return BuiltInRegistries.SOUND_EVENT.getOptional(FIERY_EXPLOSION_SOUND);
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    /** Same finish anim as HeatSurge cast */
    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.TOUCH_GROUND_ANIMATION;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level.isClientSide) {
            return;
        }

        // HeatSurge.onCast equivalent
        MagicManager.spawnParticles(
                level,
                new BlastwaveParticleOptions(SchoolRegistry.FIRE.get().getTargetingColor(), RADIUS),
                entity.getX(),
                entity.getY() + 0.165f,
                entity.getZ(),
                1,
                0,
                0,
                0,
                0,
                true);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                entity,
                new ShockwaveParticlesPacket(
                        new Vec3(entity.getX(), entity.getY() + 0.165f, entity.getZ()),
                        RADIUS,
                        ParticleRegistry.FIRE_PARTICLE.get()));

        // IN_FIRE type, direct entity = caster (kill credit / loot / stats)
        var inFire = level.damageSources().source(DamageTypes.IN_FIRE, entity);
        level.getEntities(entity, entity.getBoundingBox().inflate(RADIUS, RADIUS, RADIUS), target -> {
            if (!(target instanceof LivingEntity living) || !living.isAlive() || living.isSpectator()) {
                return false;
            }
            if (living.getRemainingFireTicks() <= 0) {
                return false;
            }
            if (DamageSources.isFriendlyFireBetween(living, entity)) {
                return false;
            }
            return living.distanceToSqr(entity) <= RADIUS_SQR;
        }).forEach(target -> {
            if (!(target instanceof LivingEntity living)) {
                return;
            }
            int t = living.getRemainingFireTicks();
            if (t <= 0) {
                return;
            }
            float damage = DAMAGE_PER_TICK_PER_LEVEL * t * spellLevel;
            if (damage > 0f) {
                living.hurt(inFire, damage);
            }
            living.setRemainingFireTicks(-1);
            living.addEffect(new MobEffectInstance(MobEffects.UNLUCK, UNLUCK_DURATION_TICKS, UNLUCK_AMPLIFIER, false, true, true));
            MagicManager.spawnParticles(
                    level,
                    ParticleHelper.EMBERS,
                    living.getX(),
                    living.getY() + living.getBbHeight() * 0.5f,
                    living.getZ(),
                    50,
                    living.getBbWidth() * 0.5f,
                    living.getBbHeight() * 0.5f,
                    living.getBbWidth() * 0.5f,
                    0.03,
                    false);
        });

        // Regen amplifier: spell level I-VI -> 0-5
        int amplifier = Mth.clamp(spellLevel - 1, 0, 5);
        entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_DURATION_TICKS, amplifier, false, true, true));
        entity.addEffect(new MobEffectInstance(MobEffects.LUCK, LUCK_DURATION_TICKS, LUCK_AMPLIFIER, false, true, true));

        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
