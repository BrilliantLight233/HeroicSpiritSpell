package com.lin.heroic_spirit_spell.entity.spells;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.particle.TraceParticleOptions;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TraceableEntity;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Moon-light style charge cut entity (server-side hit logic). Client mesh in EnderMoonlightChargeCutRenderer.
 */
public class EnderMoonlightChargeCutEntity extends Entity implements TraceableEntity {

    public static final int PROCESS_START_DELAY_TICKS = 2;
    public static final int PROCESS_DURATION_TICKS = 15;
    public static final float START_OFFSET_BLOCKS = 2.0f;
    public static final float SURFACE_OFFSET_BLOCKS = 0.02f;
    /** Max travel distance for the charge cut (aligned with moon_light scaling target; was 16, now 24). */
    public static final float FIXED_DISTANCE_BLOCKS = 24.0f;

    public static final float DAMAGE_WIDTH_BLOCKS = 0.9f;
    public static final float DAMAGE_HALF_WIDTH_BLOCKS = DAMAGE_WIDTH_BLOCKS * 0.5f;
    public static final float AREA_HEIGHT_BLOCKS = 6.0f;
    public static final float V_NOTCH_ANGLE_DEGREES = 30.0f;
    public static final float MIN_NOTCH_DEPTH = 0.0f;
    public static final float MAX_NOTCH_DEPTH = 4.0f;
    /** Smaller near width => tighter gap between left/right halves at the V notch. */
    public static final float VISUAL_NEAR_WIDTH_BLOCKS = 0.0f;
    public static final float VISUAL_NEAR_HALF_WIDTH_BLOCKS = VISUAL_NEAR_WIDTH_BLOCKS * 0.5f;
    public static final float VISUAL_FAR_WIDTH_BLOCKS = 0.7f;
    public static final float VISUAL_FAR_HALF_WIDTH_BLOCKS = VISUAL_FAR_WIDTH_BLOCKS * 0.5f;

    private static final float SEGMENT_MARGIN_BLOCKS = 0.05f;

    /** Same layout counts as apprentice_codex MoonLightChargeCutEntity portal trail (particle type swapped to Trace). */
    private static final int TRACE_PARTICLE_COUNT_PER_EMITTER = 1;
    private static final int TRACE_VERTICAL_EMITTER_COUNT = 5;

    /** irons_spellbooks ShadowSlashSpell trace tint */
    private static final Vector3f SHADOW_TRACE_COLOR = new Vector3f(1f, 0.333f, 1f);

    private static final EntityDataAccessor<Float> DATA_DISTANCE_BLOCKS =
            SynchedEntityData.defineId(EnderMoonlightChargeCutEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_PROCESSED_DISTANCE =
            SynchedEntityData.defineId(EnderMoonlightChargeCutEntity.class, EntityDataSerializers.FLOAT);

    private Entity owner;
    private float damage;
    private float previousProcessedDistance;
    private final Set<UUID> damagedEntityIds = ConcurrentHashMap.newKeySet();

    public EnderMoonlightChargeCutEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        setNoGravity(true);
    }

    public EnderMoonlightChargeCutEntity(EntityType<?> entityType, Level level, Entity owner) {
        this(entityType, level);
        this.owner = owner;
    }

    @Override
    public Entity getOwner() {
        return owner;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_DISTANCE_BLOCKS, 0.0F);
        builder.define(DATA_PROCESSED_DISTANCE, 0.0F);
    }

    @Override
    public void tick() {
        Level level = level();
        previousProcessedDistance = getProcessedDistance();
        super.tick();

        if (level.isClientSide) {
            return;
        }

        if (!(owner instanceof LivingEntity livingOwner) || owner.isRemoved()) {
            discard();
            return;
        }

        float maxDistance = getDistanceBlocks();
        if (maxDistance <= 0.0f) {
            discard();
            return;
        }

        if (tickCount <= PROCESS_START_DELAY_TICKS) {
            return;
        }

        int elapsedTicks = tickCount - PROCESS_START_DELAY_TICKS;
        float normalizedProgress = Mth.clamp(
                elapsedTicks / (float) Math.max(1, PROCESS_DURATION_TICKS),
                0.0f,
                1.0f
        );
        float easedProgress = easeOutCubic(normalizedProgress);
        float currentDistance = getProcessedDistance();
        float nextDistance = Math.min(maxDistance, maxDistance * easedProgress);
        if (nextDistance > currentDistance) {
            if (level instanceof ServerLevel serverLevel) {
                applyDamageAlongSegment(serverLevel, livingOwner, currentDistance, nextDistance);
            }
            setProcessedDistance(nextDistance);
        }

        if (tickCount > PROCESS_START_DELAY_TICKS && level instanceof ServerLevel serverLevel) {
            spawnShadowSlashStyleTrailServer(serverLevel);
        }

        if (normalizedProgress >= 1.0f || nextDistance >= maxDistance) {
            discard();
        }
    }

    private static float easeOutCubic(float value) {
        float t = Mth.clamp(value, 0.0f, 1.0f);
        float inverse = 1.0f - t;
        return 1.0f - inverse * inverse * inverse;
    }

    /** HP ratio damage multiplier: ~1x at full HP, 2x at <=50% max HP. */
    static float healthDamageMultiplier(LivingEntity target) {
        float max = target.getMaxHealth();
        if (max <= 1e-6f) {
            return 2f;
        }
        float r = target.getHealth() / max;
        if (r <= 0.5f) {
            return 2f;
        }
        return 1f + (1f - r) / 0.5f;
    }

    private void applyDamageAlongSegment(ServerLevel level, LivingEntity owner, float segmentStart, float segmentEnd) {
        if (segmentEnd <= segmentStart) {
            return;
        }

        Vec3 startPos = position();
        Vec3 forward = getForwardDirection();
        Vec3 right = calculateRightDirection(forward);
        Vec3 up = calculateUpDirection(forward, right);
        var damageSource = level.damageSources().source(DamageTypes.MAGIC, owner);

        for (LivingEntity target : level.getEntitiesOfClass(
                LivingEntity.class,
                getBoundingBoxForCulling(),
                t -> t != owner
                        && t.isAlive()
                        && !damagedEntityIds.contains(t.getUUID())
                        && !t.isSpectator())) {
            if (!isInsideSegment(target.getBoundingBox(), startPos, forward, right, up, segmentStart, segmentEnd)) {
                continue;
            }
            damagedEntityIds.add(target.getUUID());
            float amt = damage * healthDamageMultiplier(target);
            if (DamageSources.applyDamage(target, amt, damageSource)) {
                EnchantmentHelper.doPostAttackEffects(level, target, damageSource);
                Vec3 knockback = target.position().subtract(owner.position()).normalize().add(0, 0.5, 0).normalize();
                knockback = knockback.scale(Utils.random.nextIntBetweenInclusive(70, 100) / 100f
                        * Utils.clampedKnockbackResistanceFactor(target, .2f, 1f) * .1f);
                target.setDeltaMovement(target.getDeltaMovement().add(knockback));
                target.hurtMarked = true;
            }
        }
    }

    /**
     * Same emitter layout as apprentice_codex {@code MoonLightChargeCutEntity.spawnPortalParticlesClient},
     * but uses ShadowSlash-style {@link TraceParticleOptions} (server so all players see the trail).
     */
    private void spawnShadowSlashStyleTrailServer(ServerLevel level) {
        Vec3 forward = getForwardDirection();
        Vec3 right = calculateRightDirection(forward);
        Vec3 up = calculateUpDirection(forward, right);
        Vec3 frontCenter = position().add(forward.scale(getProcessedDistance()));
        float notchDepth = getNotchDepth();
        Vec3 frontDepthOffset = forward.scale(-notchDepth);
        Vec3 leftOffset = right.scale(-VISUAL_FAR_HALF_WIDTH_BLOCKS);
        Vec3 rightOffset = right.scale(VISUAL_FAR_HALF_WIDTH_BLOCKS);
        Vec3 bottomLeft = frontCenter.add(frontDepthOffset).add(leftOffset);
        Vec3 bottomRight = frontCenter.add(frontDepthOffset).add(rightOffset);
        int verticalSegments = Math.max(1, TRACE_VERTICAL_EMITTER_COUNT - 1);
        for (int i = 0; i <= verticalSegments; ++i) {
            double ratio = i / (double) verticalSegments;
            Vec3 verticalOffset = up.scale(AREA_HEIGHT_BLOCKS * ratio);
            float verticalBias = Mth.lerp((float) ratio, -0.015f, 0.015f);
            spawnTraceEmitterServer(level, bottomLeft.add(verticalOffset), forward, 0.025, verticalBias);
            spawnTraceEmitterServer(level, bottomRight.add(verticalOffset), forward, 0.025, verticalBias);
        }
    }

    /** ShadowSlash trail: short Trace segment per sample (see ShadowSlashSpell onCast). */
    private void spawnTraceEmitterServer(ServerLevel level, Vec3 origin, Vec3 forward, double lateralScale, double verticalBias) {
        var random = level.getRandom();
        for (int i = 0; i < TRACE_PARTICLE_COUNT_PER_EMITTER; ++i) {
            double jitterX = (random.nextDouble() - 0.5) * 0.05;
            double jitterY = (random.nextDouble() - 0.5) * 0.05;
            double jitterZ = (random.nextDouble() - 0.5) * 0.05;
            Vec3 start = origin.add(new Vec3(jitterX, jitterY, jitterZ));
            Vec3 particleEnd = start.add(forward.scale(0.12 + random.nextDouble() * 0.2));
            particleEnd = particleEnd.add(
                    (random.nextDouble() - 0.5) * lateralScale,
                    verticalBias + (random.nextDouble() - 0.5) * 0.02,
                    (random.nextDouble() - 0.5) * lateralScale);
            float speed = 0.1f;
            MagicManager.spawnParticles(level,
                    new TraceParticleOptions(Utils.v3f(particleEnd), SHADOW_TRACE_COLOR),
                    start.x, start.y, start.z, 1, 0, 0, 0, speed, false);
        }
    }

    private float getNotchDepth() {
        float halfAngleRad = (V_NOTCH_ANGLE_DEGREES * 0.5f) * Mth.DEG_TO_RAD;
        float tanHalf = (float) Math.tan(halfAngleRad);
        if (tanHalf <= 1.0e-4f) {
            return Mth.clamp(0.05f, Math.max(MIN_NOTCH_DEPTH, 0.05f), MAX_NOTCH_DEPTH);
        }
        return Mth.clamp(VISUAL_FAR_HALF_WIDTH_BLOCKS / tanHalf, Math.max(MIN_NOTCH_DEPTH, 0.05f), MAX_NOTCH_DEPTH);
    }

    public boolean isProcessingStarted() {
        return tickCount > PROCESS_START_DELAY_TICKS;
    }

    public float getProcessedDistanceForRender(float partialTick) {
        return Mth.lerp(partialTick, previousProcessedDistance, getProcessedDistance());
    }

    private boolean isInsideSegment(AABB box, Vec3 startPos, Vec3 forward, Vec3 right, Vec3 up,
                                    double segmentStart, double segmentEnd) {
        ProjectionRange forwardProjection = projectAabbToAxis(box, forward);
        ProjectionRange rightProjection = projectAabbToAxis(box, right);
        ProjectionRange upProjection = projectAabbToAxis(box, up);
        double startForwardProjection = startPos.dot(forward);
        double startRightProjection = startPos.dot(right);
        double startUpProjection = startPos.dot(up);
        double minForward = forwardProjection.min() - startForwardProjection;
        double maxForward = forwardProjection.max() - startForwardProjection;
        double minRight = rightProjection.min() - startRightProjection;
        double maxRight = rightProjection.max() - startRightProjection;
        double minUp = upProjection.min() - startUpProjection;
        double maxUp = upProjection.max() - startUpProjection;

        double expandedStart = segmentStart - SEGMENT_MARGIN_BLOCKS;
        double expandedEnd = segmentEnd + SEGMENT_MARGIN_BLOCKS;
        if (maxForward < expandedStart || minForward > expandedEnd) {
            return false;
        }
        if (maxUp < -SEGMENT_MARGIN_BLOCKS || minUp > AREA_HEIGHT_BLOCKS + SEGMENT_MARGIN_BLOCKS) {
            return false;
        }

        return maxRight >= -DAMAGE_HALF_WIDTH_BLOCKS && minRight <= DAMAGE_HALF_WIDTH_BLOCKS;
    }

    private ProjectionRange projectAabbToAxis(AABB box, Vec3 axis) {
        Vec3 center = box.getCenter();
        double centerProjection = center.dot(axis);
        double halfX = box.getXsize() * 0.5;
        double halfY = box.getYsize() * 0.5;
        double halfZ = box.getZsize() * 0.5;
        double halfProjectionSize = Math.abs(axis.x) * halfX + Math.abs(axis.y) * halfY + Math.abs(axis.z) * halfZ;
        return new ProjectionRange(centerProjection - halfProjectionSize, centerProjection + halfProjectionSize);
    }

    private Vec3 getForwardDirection() {
        Vec3 direction = Vec3.directionFromRotation(getXRot(), getYRot());
        if (direction.lengthSqr() < 1.0e-6) {
            return new Vec3(0.0, 0.0, 1.0);
        }
        return direction.normalize();
    }

    private static Vec3 calculateRightDirection(Vec3 forward) {
        Vec3 right = new Vec3(0.0, 1.0, 0.0).cross(forward);
        if (right.lengthSqr() < 1.0e-6) {
            right = new Vec3(1.0, 0.0, 0.0);
        }
        return right.normalize();
    }

    private static Vec3 calculateUpDirection(Vec3 forward, Vec3 right) {
        Vec3 up = forward.cross(right);
        if (up.lengthSqr() < 1.0e-6) {
            up = new Vec3(0.0, 1.0, 0.0);
        }
        return up.normalize();
    }

    @Override
    protected void readAdditionalSaveData(@NotNull CompoundTag compound) {
        damage = compound.getFloat("Damage");
        this.entityData.set(DATA_DISTANCE_BLOCKS, compound.getFloat("DistanceBlocks"));
        float processedDistance = compound.getFloat("ProcessedDistance");
        this.entityData.set(DATA_PROCESSED_DISTANCE, processedDistance);
        previousProcessedDistance = processedDistance;
    }

    @Override
    protected void addAdditionalSaveData(@NotNull CompoundTag compound) {
        compound.putFloat("Damage", damage);
        compound.putFloat("DistanceBlocks", getDistanceBlocks());
        compound.putFloat("ProcessedDistance", getProcessedDistance());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double dist) {
        return dist < 128 * 128;
    }

    @Override
    public @NotNull AABB getBoundingBoxForCulling() {
        Vec3 direction = getForwardDirection();
        Vec3 right = calculateRightDirection(direction);
        Vec3 up = calculateUpDirection(direction, right);
        Vec3 start = position();
        float horizontalHalfWidth = Math.max(VISUAL_FAR_HALF_WIDTH_BLOCKS, DAMAGE_HALF_WIDTH_BLOCKS);
        return createOrientedBounds(start, direction, right, up, getDistanceBlocks(), horizontalHalfWidth, AREA_HEIGHT_BLOCKS)
                .inflate(0.2);
    }

    private static AABB createOrientedBounds(Vec3 start, Vec3 forward, Vec3 right, Vec3 up,
                                           float distance, float halfWidth, float height) {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        for (double forwardLength : new double[]{0.0, distance}) {
            for (double side : new double[]{-halfWidth, halfWidth}) {
                for (double vertical : new double[]{0.0, height}) {
                    Vec3 point = start
                            .add(forward.scale(forwardLength))
                            .add(right.scale(side))
                            .add(up.scale(vertical));
                    minX = Math.min(minX, point.x);
                    minY = Math.min(minY, point.y);
                    minZ = Math.min(minZ, point.z);
                    maxX = Math.max(maxX, point.x);
                    maxY = Math.max(maxY, point.y);
                    maxZ = Math.max(maxZ, point.z);
                }
            }
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public void setup(float distanceBlocks, float damage) {
        this.damage = Math.max(0.0f, damage);
        setDistanceBlocks(distanceBlocks);
        setProcessedDistance(0.0f);
        previousProcessedDistance = 0.0f;
    }

    public float getDistanceBlocks() {
        return this.entityData.get(DATA_DISTANCE_BLOCKS);
    }

    public float getProcessedDistance() {
        return this.entityData.get(DATA_PROCESSED_DISTANCE);
    }

    private void setDistanceBlocks(float distanceBlocks) {
        this.entityData.set(DATA_DISTANCE_BLOCKS, Math.max(0.0f, distanceBlocks));
    }

    private void setProcessedDistance(float processedDistance) {
        this.entityData.set(DATA_PROCESSED_DISTANCE, Mth.clamp(processedDistance, 0.0f, getDistanceBlocks()));
    }

    private record ProjectionRange(double min, double max) {}
}
