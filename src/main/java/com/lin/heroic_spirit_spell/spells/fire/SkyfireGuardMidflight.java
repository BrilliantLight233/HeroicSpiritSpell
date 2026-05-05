package com.lin.heroic_spirit_spell.spells.fire;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.registry.ModEffects;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.entity.spells.target_area.TargetedAreaEntity;
import io.redspace.ironsspellbooks.player.SpinAttackType;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;

/**
 * Skyfire Guard mid phase: upward burst, then delayed phasing flight to target-above point.
 */
public final class SkyfireGuardMidflight {
    public static final String ACTIVE_KEY = HeroicSpiritSpell.MODID + ":skyfire_guard_mid_active";
    private static final String PHASE_KEY = HeroicSpiritSpell.MODID + ":skyfire_guard_phase";
    private static final String SPELL_LEVEL_KEY = HeroicSpiritSpell.MODID + ":skyfire_guard_spell_level";
    private static final int PHASE_TRAVEL = 0;
    private static final int PHASE_DIVE = 1;
    private static final String DELAY_KEY = HeroicSpiritSpell.MODID + ":skyfire_guard_mid_delay";
    private static final String ASCEND_TARGET_Y_KEY = HeroicSpiritSpell.MODID + ":skyfire_guard_mid_ascend_target_y";
    private static final String TARGET_X_KEY = HeroicSpiritSpell.MODID + ":skyfire_guard_mid_target_x";
    private static final String TARGET_Y_KEY = HeroicSpiritSpell.MODID + ":skyfire_guard_mid_target_y";
    private static final String TARGET_Z_KEY = HeroicSpiritSpell.MODID + ":skyfire_guard_mid_target_z";
    private static final String CENTER_X_KEY = HeroicSpiritSpell.MODID + ":skyfire_guard_center_x";
    private static final String CENTER_Y_KEY = HeroicSpiritSpell.MODID + ":skyfire_guard_center_y";
    private static final String CENTER_Z_KEY = HeroicSpiritSpell.MODID + ":skyfire_guard_center_z";
    private static final String AREA_ENTITY_ID_KEY = HeroicSpiritSpell.MODID + ":skyfire_guard_area_entity_id";

    private static final int DELAY_TICKS = 10;
    private static final int PRE_DIVE_INCORPOREITY_TICKS = 7; // 7tick
    private static final double UPWARD_ASCEND_BLOCKS = 8.0;
    private static final double UPWARD_ASCEND_SPEED_PER_TICK = 1.2;
    private static final int BURNING_DASH_STATE_AMPLIFIER = 1; // level II
    private static final double TRAVEL_SPEED_BLOCKS_PER_TICK = 2.4;
    private static final double DIVE_SPEED_BLOCKS_PER_TICK = 2.8;
    private static final double ARRIVE_EPSILON = 0.75;
    private static final double TARGET_SNAP_RADIUS = 1.5;
    private static final double ERUPTION_RADIUS = 8.0;
    private static final int UNLUCK_DURATION_TICKS = 20 * 5;
    private static final int UNLUCK_AMPLIFIER = 3; // IV
    private static final int TRAIL_PARTICLE_COUNT = 6;
    private static final int LOOP_SOUND_INTERVAL_TICKS = 8;
    private static final ResourceLocation FIRE_BREATH_LOOP_ID = ResourceLocation.parse("irons_spellbooks:fire_breath_loop");
    private static final ResourceLocation FIERY_EXPLOSION_ID = ResourceLocation.parse("irons_spellbooks:fiery_explosion_1");

    private SkyfireGuardMidflight() {
    }

    public static void start(LivingEntity caster, Vec3 targetAbove, Vec3 targetCenter, int spellLevel, TargetedAreaEntity targetArea) {
        caster.getPersistentData().putBoolean(ACTIVE_KEY, true);
        caster.getPersistentData().putInt(PHASE_KEY, PHASE_TRAVEL);
        caster.getPersistentData().putInt(SPELL_LEVEL_KEY, spellLevel);
        caster.getPersistentData().putInt(DELAY_KEY, DELAY_TICKS);
        caster.getPersistentData().putDouble(ASCEND_TARGET_Y_KEY, caster.getY() + UPWARD_ASCEND_BLOCKS);
        caster.getPersistentData().putDouble(TARGET_X_KEY, targetAbove.x);
        caster.getPersistentData().putDouble(TARGET_Y_KEY, targetAbove.y);
        caster.getPersistentData().putDouble(TARGET_Z_KEY, targetAbove.z);
        caster.getPersistentData().putDouble(CENTER_X_KEY, targetCenter.x);
        caster.getPersistentData().putDouble(CENTER_Y_KEY, targetCenter.y);
        caster.getPersistentData().putDouble(CENTER_Z_KEY, targetCenter.z);
        if (targetArea != null && !targetArea.isRemoved()) {
            caster.getPersistentData().putInt(AREA_ENTITY_ID_KEY, targetArea.getId());
        }

        // Phase 1: upward burst, equivalent to Burning Dash 0-level motion profile.
        Vec3 upwardImpulse = buildUpwardDashImpulse(caster);
        caster.hasImpulse = true;
        caster.setDeltaMovement(new Vec3(
                Mth.lerp(0.75f, caster.getDeltaMovement().x, upwardImpulse.x),
                Mth.lerp(0.75f, caster.getDeltaMovement().y, upwardImpulse.y),
                Mth.lerp(0.75f, caster.getDeltaMovement().z, upwardImpulse.z)));
        caster.hurtMarked = true;

        applyBurningDashState(caster, true);
        faceDirection(caster, upwardImpulse.normalize());
    }

    public static void tick(LivingEntity caster) {
        if (!caster.getPersistentData().getBoolean(ACTIVE_KEY)) {
            return;
        }
        int phase = caster.getPersistentData().getInt(PHASE_KEY);
        if (phase == PHASE_DIVE) {
            playFlightLoopSound(caster);
            tickDivePhase(caster);
            return;
        }

        playFlightLoopSound(caster);
        applyBurningDashState(caster, true);

        int delay = caster.getPersistentData().getInt(DELAY_KEY);
        if (delay > 0) {
            double ascendTargetY = caster.getPersistentData().getDouble(ASCEND_TARGET_Y_KEY);
            double remainUp = ascendTargetY - caster.getY();
            if (remainUp > 0.01) {
                double step = Math.min(UPWARD_ASCEND_SPEED_PER_TICK, remainUp);
                Vec3 moveUp = new Vec3(0.0, step, 0.0);
                caster.fallDistance = 0;
                caster.hasImpulse = true;
                caster.setDeltaMovement(moveUp);
                caster.hurtMarked = true;
                caster.moveTo(caster.getX(), caster.getY() + step, caster.getZ(), caster.getYRot(), caster.getXRot());
                faceDirection(caster, new Vec3(0.0, 1.0, 0.0));
                spawnBurningDashTrail(caster, new Vec3(0.0, 1.0, 0.0));
            }
            caster.getPersistentData().putInt(DELAY_KEY, delay - 1);
            return;
        }

        Vec3 target = new Vec3(
                caster.getPersistentData().getDouble(TARGET_X_KEY),
                caster.getPersistentData().getDouble(TARGET_Y_KEY),
                caster.getPersistentData().getDouble(TARGET_Z_KEY));
        Vec3 origin = caster.position();
        double snapR2 = TARGET_SNAP_RADIUS * TARGET_SNAP_RADIUS;
        if (origin.distanceToSqr(target) <= snapR2) {
            caster.teleportTo(target.x, target.y, target.z);
            beginDivePhase(caster);
            return;
        }
        Vec3 to = target.subtract(origin);
        double dist = to.length();
        if (dist <= ARRIVE_EPSILON) {
            caster.teleportTo(target.x, target.y, target.z);
            beginDivePhase(caster);
            return;
        }

        Vec3 direction = to.normalize();
        Vec3 motion = direction.scale(Math.min(TRAVEL_SPEED_BLOCKS_PER_TICK, dist));

        caster.fallDistance = 0;
        caster.noPhysics = true;
        caster.setOnGround(false);
        caster.hasImpulse = true;
        caster.setDeltaMovement(motion);
        caster.hurtMarked = true;
        Vec3 nextPos = caster.position().add(motion);
        caster.moveTo(nextPos.x, nextPos.y, nextPos.z, caster.getYRot(), caster.getXRot());
        spawnBurningDashTrail(caster, motion.normalize());
        faceDirection(caster, direction);
    }

    private static void beginDivePhase(LivingEntity caster) {
        caster.getPersistentData().putInt(PHASE_KEY, PHASE_DIVE);
        caster.addEffect(new MobEffectInstance(
                ModEffects.INCORPOREITY,
                PRE_DIVE_INCORPOREITY_TICKS,
                0,
                false,
                false,
                false));
        caster.noPhysics = false;
        caster.setOnGround(false);
        Vec3 down = new Vec3(0.0, -DIVE_SPEED_BLOCKS_PER_TICK, 0.0);
        caster.hasImpulse = true;
        caster.setDeltaMovement(down);
        caster.hurtMarked = true;
        faceDirection(caster, down);
        applyBurningDashState(caster, false);
    }

    private static void tickDivePhase(LivingEntity caster) {
        boolean hasIncorporeity = caster.hasEffect(ModEffects.INCORPOREITY);
        // During dive, keep Burning Dash visuals/state but do not refresh incorporeity duration.
        // This preserves "phase through blocks first, then land and erupt" sequencing.
        applyBurningDashState(caster, false);
        if (!hasIncorporeity && caster.onGround()) {
            executeLandingEruption(caster);
            clear(caster, true);
            return;
        }
        Vec3 down = new Vec3(0.0, -DIVE_SPEED_BLOCKS_PER_TICK, 0.0);
        caster.fallDistance = 0;
        if (hasIncorporeity) {
            caster.setOnGround(false);
        }
        caster.hasImpulse = true;
        // No forced horizontal repositioning: preserve natural motion and avoid fixed-point oscillation.
        caster.setDeltaMovement(down);
        caster.hurtMarked = true;
        faceDirection(caster, down);
        spawnBurningDashTrail(caster, down.normalize());
    }

    public static void clear(LivingEntity caster, boolean restorePhysics) {
        discardTargetArea(caster);
        caster.getPersistentData().remove(ACTIVE_KEY);
        caster.getPersistentData().remove(PHASE_KEY);
        caster.getPersistentData().remove(SPELL_LEVEL_KEY);
        caster.getPersistentData().remove(DELAY_KEY);
        caster.getPersistentData().remove(ASCEND_TARGET_Y_KEY);
        caster.getPersistentData().remove(TARGET_X_KEY);
        caster.getPersistentData().remove(TARGET_Y_KEY);
        caster.getPersistentData().remove(TARGET_Z_KEY);
        caster.getPersistentData().remove(CENTER_X_KEY);
        caster.getPersistentData().remove(CENTER_Y_KEY);
        caster.getPersistentData().remove(CENTER_Z_KEY);
        caster.getPersistentData().remove(AREA_ENTITY_ID_KEY);
        if (restorePhysics) {
            caster.noPhysics = false;
        }
    }

    private static void applyBurningDashState(LivingEntity caster, boolean addIncorporeity) {
        caster.addEffect(new MobEffectInstance(
                MobEffectRegistry.BURNING_DASH,
                5,
                BURNING_DASH_STATE_AMPLIFIER,
                false,
                false,
                false));
        caster.invulnerableTime = 20;
        if (caster instanceof Player player) {
            MagicData.getPlayerMagicData(player).getSyncedData().setSpinAttackType(SpinAttackType.FIRE);
        }
        if (addIncorporeity) {
            caster.addEffect(new MobEffectInstance(
                    ModEffects.INCORPOREITY,
                    5,
                    0,
                    false,
                    false,
                    false));
        }
    }

    private static Vec3 buildUpwardDashImpulse(LivingEntity caster) {
        float multiplier = (15f + 0f) / 12f;
        Vec3 forward = new Vec3(0.0, 1.0, 0.0);
        Vec3 impulse = forward.multiply(3.0, 1.0, 3.0).normalize().add(0.0, 0.25, 0.0).scale(multiplier);
        if (caster.onGround()) {
            caster.setPos(caster.position().add(0.0, 1.5, 0.0));
            impulse = impulse.add(0.0, 0.25, 0.0);
        }
        return impulse;
    }

    private static void spawnBurningDashTrail(LivingEntity caster, Vec3 dir) {
        Vec3 center = caster.getBoundingBox().getCenter();
        Vec3 backward = dir.lengthSqr() < 1e-6 ? Vec3.ZERO : dir.scale(-0.55);
        Vec3 base = center.add(backward);
        MagicManager.spawnParticles(
                caster.level(),
                ParticleHelper.FIRE,
                base.x, base.y, base.z,
                TRAIL_PARTICLE_COUNT,
                0.25, 0.25, 0.25,
                0.03,
                false);
        MagicManager.spawnParticles(
                caster.level(),
                ParticleHelper.FIERY_SMOKE,
                base.x, base.y, base.z,
                TRAIL_PARTICLE_COUNT / 2,
                0.2, 0.2, 0.2,
                0.015,
                false);
    }

    private static void faceDirection(LivingEntity entity, Vec3 dir) {
        if (dir.lengthSqr() < 1.0e-8) {
            return;
        }
        float yaw = (float) (Mth.atan2(dir.z, dir.x) * (180f / Math.PI)) - 90f;
        float pitch = (float) (-(Mth.atan2(dir.y, Math.sqrt(dir.x * dir.x + dir.z * dir.z)) * (180f / Math.PI)));
        entity.setYRot(yaw);
        entity.setXRot(pitch);
        entity.setYHeadRot(yaw);
        entity.setYBodyRot(yaw);
    }

    private static void executeLandingEruption(LivingEntity caster) {
        playLandingExplosionSound(caster);
        int spellLevel = Math.max(1, caster.getPersistentData().getInt(SPELL_LEVEL_KEY));
        double radius = ERUPTION_RADIUS;
        double r2 = radius * radius;
        Vec3 center = new Vec3(caster.getX(), caster.getBoundingBox().minY, caster.getZ());
        AABB box = caster.getBoundingBox().inflate(radius, 3.0, radius);
        Entity damageSourceEntity = null;

        // Spawn irons_spellbooks:fire_eruption at feet (raise_hell-like direct eruption spawning).
        EntityType<?> fireEruptionType = BuiltInRegistries.ENTITY_TYPE.getOptional(
                ResourceLocation.parse("irons_spellbooks:fire_eruption")).orElse(null);
        if (fireEruptionType != null) {
            Entity eruption = fireEruptionType.create(caster.level());
            if (eruption != null) {
                // Spawn at feet with a small vertical offset to avoid clipping into floor blocks.
                double eruptionY = center.y + 0.05;
                eruption.moveTo(center.x, eruptionY, center.z, caster.getYRot(), 0f);
                bindEruptionOwner(eruption, caster);
                applyEruptionRadiusIfSupported(eruption, (float) ERUPTION_RADIUS);
                configureEruptionLifecycleIfSupported(eruption);
                caster.level().addFreshEntity(eruption);
                damageSourceEntity = eruption;
            }
        }

        float damage = 5f * spellLevel;
        for (LivingEntity target : caster.level().getEntitiesOfClass(LivingEntity.class, box)) {
            if (target == caster || target.isAlliedTo(caster) || target.position().distanceToSqr(center) > r2) {
                continue;
            }
            Entity source = damageSourceEntity != null ? damageSourceEntity : caster;
            target.hurt(caster.damageSources().indirectMagic(source, caster), damage);
            target.setDeltaMovement(target.getDeltaMovement().x, Math.max(target.getDeltaMovement().y, 0.9), target.getDeltaMovement().z);
            target.hurtMarked = true;
            target.addEffect(new MobEffectInstance(MobEffects.UNLUCK, UNLUCK_DURATION_TICKS, UNLUCK_AMPLIFIER, false, true, true));
        }
        MagicManager.spawnParticles(
                caster.level(),
                ParticleHelper.FIERY_SPARKS,
                center.x, center.y, center.z,
                40, radius * 0.45, 0.5, radius * 0.45,
                0.08, false);
    }

    private static void discardTargetArea(LivingEntity caster) {
        if (!caster.getPersistentData().contains(AREA_ENTITY_ID_KEY)) {
            return;
        }
        int id = caster.getPersistentData().getInt(AREA_ENTITY_ID_KEY);
        Entity entity = caster.level().getEntity(id);
        if (entity instanceof TargetedAreaEntity area && !area.isRemoved()) {
            area.discard();
        }
    }

    private static void applyEruptionRadiusIfSupported(Entity eruption, float radius) {
        try {
            Method m = eruption.getClass().getMethod("setRadius", float.class);
            m.invoke(eruption, radius);
            return;
        } catch (ReflectiveOperationException ignored) {
            // try double signature below
        }
        try {
            Method m = eruption.getClass().getMethod("setRadius", double.class);
            m.invoke(eruption, (double) radius);
        } catch (ReflectiveOperationException ignored) {
            // No accessible radius setter in this mapping/environment.
        }
    }

    private static void bindEruptionOwner(Entity eruption, LivingEntity caster) {
        try {
            Method m = eruption.getClass().getMethod("setOwner", Entity.class);
            m.invoke(eruption, caster);
            return;
        } catch (ReflectiveOperationException ignored) {
            // try LivingEntity signature below
        }
        try {
            Method m = eruption.getClass().getMethod("setOwner", LivingEntity.class);
            m.invoke(eruption, caster);
        } catch (ReflectiveOperationException ignored) {
            // Best effort; some mappings hide/set owner differently.
        }
    }

    private static void configureEruptionLifecycleIfSupported(Entity eruption) {
        try {
            Method wait = eruption.getClass().getMethod("setWaitTime", int.class);
            wait.invoke(eruption, 0);
        } catch (ReflectiveOperationException ignored) {
            // Some mappings may not expose this method.
        }
        try {
            Method duration = eruption.getClass().getMethod("setDuration", int.class);
            duration.invoke(eruption, 12);
        } catch (ReflectiveOperationException ignored) {
            // Some mappings may not expose this method.
        }
        try {
            Method durationOnUse = eruption.getClass().getMethod("setDurationOnUse", int.class);
            durationOnUse.invoke(eruption, 0);
        } catch (ReflectiveOperationException ignored) {
            // Optional lifecycle tuning.
        }
        try {
            Method radiusOnUse = eruption.getClass().getMethod("setRadiusOnUse", float.class);
            radiusOnUse.invoke(eruption, 0f);
        } catch (ReflectiveOperationException ignored) {
            // Optional lifecycle tuning.
        }
        try {
            Method radiusPerTick = eruption.getClass().getMethod("setRadiusPerTick", float.class);
            radiusPerTick.invoke(eruption, 0f);
        } catch (ReflectiveOperationException ignored) {
            // Optional lifecycle tuning.
        }
    }

    private static void playFlightLoopSound(LivingEntity caster) {
        if ((caster.tickCount % LOOP_SOUND_INTERVAL_TICKS) != 0) {
            return;
        }
        BuiltInRegistries.SOUND_EVENT.getOptional(FIRE_BREATH_LOOP_ID).ifPresent(sound ->
                caster.level().playSound(
                        null,
                        caster.getX(),
                        caster.getY(),
                        caster.getZ(),
                        sound,
                        SoundSource.PLAYERS,
                        0.85f,
                        1.0f));
    }

    private static void playLandingExplosionSound(LivingEntity caster) {
        BuiltInRegistries.SOUND_EVENT.getOptional(FIERY_EXPLOSION_ID).ifPresent(sound ->
                caster.level().playSound(
                        null,
                        caster.getX(),
                        caster.getBoundingBox().minY,
                        caster.getZ(),
                        sound,
                        SoundSource.PLAYERS,
                        1.2f,
                        1.0f));
    }
}
