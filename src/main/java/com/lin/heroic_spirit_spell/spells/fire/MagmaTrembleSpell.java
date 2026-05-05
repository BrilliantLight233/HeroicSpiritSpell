package com.lin.heroic_spirit_spell.spells.fire;

import com.github.L_Ender.cataclysm.entity.projectile.Flame_Jet_Entity;
import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.util.MagmaTrembleRuntime;
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
import io.redspace.ironsspellbooks.network.particles.ShockwaveParticlesPacket;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import io.redspace.ironsspellbooks.registries.ParticleRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public class MagmaTrembleSpell extends AbstractSpell {
    private static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(HeroicSpiritSpell.MODID, "magma_tremble");

    private static final int CAST_TICKS = 15; // 0.75s
    private static final float RADIUS = 7.0f;
    private static final float BASE_DAMAGE = 2.5f;
    private static final float DAMAGE_PER_LEVEL = 2.5f;
    private static final int IGNITE_TICKS = 60;
    private static final float KNOCKBACK_STRENGTH = 2.2f;
    private static final int FLAME_JETS_PER_DIRECTION = 6;
    private static final int FLAME_JET_DIRECTIONS = 8;
    private static final float FLAME_JET_STEP_DISTANCE = 1.2f;
    private static final int FLAME_JET_STEP_DELAY_TICKS = 2;
    private static final int SHOCKWAVE_COLOR = 0xFF8C00;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(7.5f)
            .build();

    public MagmaTrembleSpell() {
        this.castTime = CAST_TICKS;
        this.baseManaCost = 0;
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 0;
    }

    @Override
    public boolean canBeInterrupted(Player player) {
        return false;
    }

    @Override
    public int getEffectiveCastTime(int spellLevel, @Nullable LivingEntity entity) {
        // Keep fixed 0.75s cast; do not scale with cast_time_reduction.
        return getCastTime(spellLevel);
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
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
        return Optional.of(SoundRegistry.RAISE_HELL_PREPARE.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.FIRE_ERUPTION_SLAM.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.OVERHEAD_MELEE_SWING_ANIMATION;
    }

    @Override
    public AnimationHolder getCastFinishAnimation() {
        return AnimationHolder.pass();
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getDamage(spellLevel), 1)),
                Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(RADIUS, 1))
        );
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData playerMagicData) {
        if (level.isClientSide) {
            return;
        }

        Vec3 center = new Vec3(caster.getX(), caster.getBoundingBox().minY + 0.1, caster.getZ());
        float damage = getDamage(spellLevel);

        MagicManager.spawnParticles(
                level,
                new BlastwaveParticleOptions(Utils.deconstructRGB(SHOCKWAVE_COLOR), RADIUS),
                center.x,
                center.y,
                center.z,
                1,
                0,
                0,
                0,
                0,
                true
        );
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                caster,
                new ShockwaveParticlesPacket(center, RADIUS, ParticleRegistry.FIRE_PARTICLE.get()));
        level.playSound(null, caster.getX(), caster.getY(), caster.getZ(), SoundRegistry.ELDRITCH_BLAST.get(), caster.getSoundSource(), 1.0f, 1.0f);
        MagmaTrembleRuntime.start(level, caster, center, (int) RADIUS, damage, IGNITE_TICKS, KNOCKBACK_STRENGTH);

        spawnFlameJets(level, caster, center);
        super.onCast(level, spellLevel, caster, castSource, playerMagicData);
    }

    private static void spawnFlameJets(Level level, LivingEntity caster, Vec3 center) {
        for (int step = 1; step <= FLAME_JETS_PER_DIRECTION; step++) {
            float distance = step * FLAME_JET_STEP_DISTANCE;
            int warmup = (step - 1) * FLAME_JET_STEP_DELAY_TICKS;
            for (int dirIndex = 0; dirIndex < FLAME_JET_DIRECTIONS; dirIndex++) {
                double angle = (Math.PI * 2.0 * dirIndex) / FLAME_JET_DIRECTIONS;
                double offsetX = Math.cos(angle) * distance;
                double offsetZ = Math.sin(angle) * distance;
                var flameJet = new Flame_Jet_Entity(level, center.x + offsetX, center.y, center.z + offsetZ, (float) Math.toDegrees(angle), warmup, 0f, caster);
                flameJet.setDamage(0f);
                flameJet.setCaster(caster);
                level.addFreshEntity(flameJet);
            }
        }
    }

    private float getDamage(int spellLevel) {
        return BASE_DAMAGE + DAMAGE_PER_LEVEL * (spellLevel - 1);
    }

    @Override
    public boolean shouldAIStopCasting(int spellLevel, Mob mob, LivingEntity target) {
        float stopDistance = RADIUS * 1.25f;
        return mob.distanceToSqr(target) > stopDistance * stopDistance;
    }
}
