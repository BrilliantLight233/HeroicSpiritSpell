package com.lin.heroic_spirit_spell.mixin;

import com.lin.heroic_spirit_spell.mixin.accessor.StarfallCastDataAccessor;
import com.lin.heroic_spirit_spell.util.StarfallRuntimeManager;
import com.lin.heroic_spirit_spell.util.StarfallRuntimeState;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.comet.Comet;
import io.redspace.ironsspellbooks.entity.spells.target_area.TargetedAreaEntity;
import io.redspace.ironsspellbooks.network.casting.SyncTargetingDataPacket;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import io.redspace.ironsspellbooks.spells.ender.StarfallSpell;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

@Mixin(value = StarfallSpell.class, remap = false)
public class StarfallSpellDynamicCenterMixin {
    @Unique
    private static final float HSS_STARFALL_RADIUS = 8f;
    @Unique
    private static final int HSS_STARFALL_COLOR = Utils.packRGB(new Vector3f(0.65f, 0.22f, 0.92f));
    @Unique
    private static final float HSS_FOCUS_GAIN = 1.0f;
    @Unique
    private static final float HSS_FOCUS_LOSS = 0.5f;
    @Unique
    private static final float HSS_FOCUS_MAX = 24f;
    @Unique
    private static final float HSS_CENTER_SMOOTH = 0.35f;
    @Unique
    private static final float HSS_FOCUS_RADIUS_AT_40T = 2.0f;
    @Unique
    private static final float HSS_FOCUS_TICKS_TO_RADIUS_2 = 40.0f;

    @Inject(method = "getUniqueInfo", at = @At("HEAD"), cancellable = true, remap = false)
    private void heroicSpiritSpell$overrideRadiusInfo(int spellLevel, LivingEntity caster, CallbackInfoReturnable<java.util.List<net.minecraft.network.chat.MutableComponent>> cir) {
        var self = (StarfallSpell) (Object) this;
        cir.setReturnValue(java.util.List.of(
                net.minecraft.network.chat.Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(self.getSpellPower(spellLevel, caster) * .5f, 2)),
                net.minecraft.network.chat.Component.translatable("ui.irons_spellbooks.radius", Utils.stringTruncation(HSS_STARFALL_RADIUS, 1))
        ));
    }

    @Inject(method = "onServerCastTick", at = @At("HEAD"), cancellable = true, remap = false)
    private void heroicSpiritSpell$overrideStarfallTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData, CallbackInfo ci) {
        if (playerMagicData == null || !(playerMagicData.getAdditionalCastData() instanceof StarfallSpell.StarfallCastData castData)) {
            return;
        }
        var castAccessor = (StarfallCastDataAccessor) castData;
        var runtime = StarfallRuntimeManager.getOrCreate(entity, () -> new StarfallRuntimeState(castAccessor.hss_getCenter()));

        int tick = playerMagicData.getCastDurationRemaining() - 1;
        LivingEntity focusedTarget = null;
        if (tick % 4 == 0) {
            HitResult raycast = Utils.raycastForEntity(level, entity, 40, true);
            Vec3 targetArea;
            if (raycast instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof LivingEntity targetEntity) {
                targetArea = targetEntity.position();
                focusedTarget = targetEntity;
            } else {
                targetArea = Utils.moveToRelativeGroundLevel(level, raycast.getLocation(), 12);
            }
            castAccessor.hss_setCenter(targetArea);
        }

        Vec3 center = castAccessor.hss_getCenter();
        runtime.smoothedCenter = runtime.smoothedCenter.lerp(center, HSS_CENTER_SMOOTH);
        heroicSpiritSpell$updateCenterArea(level, runtime);

        if (tick % 4 == 0) {
            if (focusedTarget != null) {
                runtime.focusedTargetId = focusedTarget.getUUID();
                runtime.focusWeight = Math.min(HSS_FOCUS_MAX, runtime.focusWeight + HSS_FOCUS_GAIN);
            } else {
                runtime.focusWeight = Math.max(0f, runtime.focusWeight - HSS_FOCUS_LOSS);
                if (runtime.focusWeight <= 0f) {
                    runtime.focusedTargetId = null;
                }
            }
            heroicSpiritSpell$syncFocusedTargetMarker(entity, runtime, focusedTarget);
        }

        if (tick % 20 == 0) {
            castData.updateTrackedEntities(level.getEntities(entity, AABB.ofSize(center, HSS_STARFALL_RADIUS * 3, HSS_STARFALL_RADIUS, HSS_STARFALL_RADIUS * 3),
                    e -> e instanceof LivingEntity && !DamageSources.isFriendlyFireBetween(entity, e)));
        }

        if (tick % 4 == 0) {
            for (int i = 0; i < 2; i++) {
                Vec3 weightedArea = heroicSpiritSpell$computeWeightedArea(castAccessor.hss_getTrackedEntities(), center, level, runtime.focusedTargetId, runtime.focusWeight);
                float focusTicks = runtime.focusedTargetId != null ? (runtime.focusWeight / HSS_FOCUS_GAIN) * 4.0f : 0.0f;
                float focusProgress = Mth.clamp(focusTicks / HSS_FOCUS_TICKS_TO_RADIUS_2, 0f, 1f);
                float randomRadiusCap = Mth.clampedLerp(HSS_STARFALL_RADIUS * .5f, HSS_FOCUS_RADIUS_AT_40T, focusProgress);
                float spawnRadius = Mth.clampedLerp(randomRadiusCap, HSS_STARFALL_RADIUS, (float) (weightedArea.length() / HSS_STARFALL_RADIUS));
                Vec3 spawnTarget = Utils.moveToRelativeGroundLevel(level,
                                center.add(weightedArea).add(new Vec3(0, 0, entity.getRandom().nextFloat() * spawnRadius).yRot(entity.getRandom().nextInt(360) * Mth.DEG_TO_RAD)),
                                3)
                        .add(0, 0.5, 0);
                Vec3 trajectory = new Vec3(.15f, -.85f, 0).normalize();
                Vec3 spawn = Utils.raycastForBlock(level, spawnTarget, spawnTarget.add(trajectory.scale(-12)), ClipContext.Fluid.NONE).getLocation().add(trajectory);
                heroicSpiritSpell$shootComet(level, spellLevel, entity, spawn, trajectory);
                MagicManager.spawnParticles(level, ParticleHelper.COMET_FOG, spawn.x, spawn.y, spawn.z, 1, 1, 1, 1, 1, false);
                MagicManager.spawnParticles(level, ParticleHelper.COMET_FOG, spawn.x, spawn.y, spawn.z, 1, 1, 1, 1, 1, true);
            }
        }
        ci.cancel();
    }

    @Unique
    private static void heroicSpiritSpell$updateCenterArea(Level level, StarfallRuntimeState runtime) {
        if (runtime.centerArea == null || runtime.centerArea.isRemoved()) {
            runtime.centerArea = TargetedAreaEntity.createTargetAreaEntity(level, runtime.smoothedCenter, HSS_STARFALL_RADIUS, HSS_STARFALL_COLOR);
            runtime.centerArea.setShouldFade(true);
            runtime.centerArea.setDuration(220);
        } else {
            runtime.centerArea.setPos(runtime.smoothedCenter);
        }
    }

    @Unique
    private static void heroicSpiritSpell$syncFocusedTargetMarker(LivingEntity caster, StarfallRuntimeState runtime, @Nullable LivingEntity target) {
        if (!(caster instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return;
        }
        UUID current = target == null ? null : target.getUUID();
        if (java.util.Objects.equals(runtime.lastSyncedTargetId, current)) {
            return;
        }
        runtime.lastSyncedTargetId = current;
        if (current == null) {
            PacketDistributor.sendToPlayer(serverPlayer, new SyncTargetingDataPacket(SpellRegistry.STARFALL_SPELL.get(), List.of()));
        } else {
            PacketDistributor.sendToPlayer(serverPlayer, new SyncTargetingDataPacket(target, SpellRegistry.STARFALL_SPELL.get()));
        }
    }

    @Unique
    private static Vec3 heroicSpiritSpell$computeWeightedArea(java.util.List<Entity> trackedEntities, Vec3 center, Level level, @Nullable UUID focusedTargetId, float focusWeight) {
        Vec3 weightedArea = Vec3.ZERO;
        for (Entity target : trackedEntities) {
            weightedArea = weightedArea.add(target.position().subtract(center));
        }
        int weightCount = trackedEntities.size();
        if (focusedTargetId != null && focusWeight > 0f) {
            Entity focused = null;
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                focused = serverLevel.getEntity(focusedTargetId);
            }
            if (focused != null && !focused.isRemoved()) {
                int extraWeight = 6 + Mth.floor(focusWeight * 3f);
                weightedArea = weightedArea.add(focused.position().subtract(center).scale(extraWeight));
                weightCount += extraWeight;
            }
        }
        if (weightCount > 0) {
            weightedArea = weightedArea.scale(1f / weightCount);
        }
        if (focusedTargetId != null && focusWeight > 0f) {
            Entity focused = null;
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                focused = serverLevel.getEntity(focusedTargetId);
            }
            if (focused != null && !focused.isRemoved()) {
                float t = Mth.clamp(0.25f + (focusWeight / HSS_FOCUS_MAX) * 0.65f, 0f, 0.9f);
                weightedArea = weightedArea.lerp(focused.position().subtract(center), t);
            }
        }
        return weightedArea;
    }

    @Unique
    private static void heroicSpiritSpell$shootComet(Level world, int spellLevel, LivingEntity entity, Vec3 spawn, Vec3 trajectory) {
        Comet comet = new Comet(world, entity);
        comet.setPos(spawn.add(-1, 0, 0));
        comet.shoot(trajectory, .075f);
        var starfall = (StarfallSpell) (Object) io.redspace.ironsspellbooks.api.registry.SpellRegistry.STARFALL_SPELL.get();
        comet.setDamage(starfall.getSpellPower(spellLevel, entity) * .5f);
        comet.setExplosionRadius(2f);
        world.addFreshEntity(comet);
        world.playSound(null, spawn.x, spawn.y, spawn.z, net.minecraft.sounds.SoundEvents.FIREWORK_ROCKET_LAUNCH, net.minecraft.sounds.SoundSource.PLAYERS, 3.0f, 0.7f + Utils.random.nextFloat() * .3f);
    }

}
