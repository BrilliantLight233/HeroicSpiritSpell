package com.lin.heroic_spirit_spell.spells.ender;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.registry.ModEffects;
import com.lin.heroic_spirit_spell.registry.ModSpells;
import com.lin.heroic_spirit_spell.util.GravitationalCollapseAirBuffs;
import com.lin.heroic_spirit_spell.util.GravitationalCollapsePending;
import com.lin.heroic_spirit_spell.util.GravityCageRuntime;
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
import io.redspace.ironsspellbooks.entity.spells.target_area.TargetedAreaEntity;
import io.redspace.ironsspellbooks.particle.TraceParticleOptions;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.spells.TargetAreaCastData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Targeted ground ring + fixed channel; teleport above and slam. Stomp-style tremor via Utils.createTremorBlock.
 */
public class GravitationalCollapseSpell extends AbstractSpell {

    private static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(HeroicSpiritSpell.MODID, "gravitational_collapse");

    private static final float PREVIEW_RADIUS = 5.0f;
    private static final double LANDING_RADIUS = 5.0;
    private static final int MAX_SELECT_DISTANCE = 32;
    private static final int CAST_TICKS = 15;
    private static final int ABOVE_TARGET_BLOCKS = 4;
    private static final double TARGET_AREA_Y_OFFSET = 1.0;
    private static final int ANTIGRAVITY_TICKS = 10;
    private static final int SPELL_GRAVITY_DELAY_TICKS = 10;
    private static final int FALL_IMMUNITY_TICKS = 55;
    private static final int ENEMY_SLOW_TICKS = 40;
    private static final int ENEMY_SLOW_AMPLIFIER = 2;
    private static final int ENEMY_GRAVITY_AMPLIFIER = 4;
    private static final int PENDING_LANDING_MAX_TICKS = 20 * 15;
    /** Pull selection center this many blocks toward caster when adjacent to solid walls. */
    private static final double WALL_AVOID_PULL_BLOCKS = 1.0;
    /** Max downward search when resolving ceiling-safe teleport feet Y. */
    private static final double TELEPORT_Y_SEARCH_DOWN = 14.0;
    private static final double TELEPORT_Y_STEP = 0.25;
    /** Blocks above selection ring center (same column) checked for fast teleport height; no extra upward scan beyond this. */
    private static final int CLEAR_COLUMN_ABOVE_SELECTION_BLOCKS = 6;

    private static final Vector3f PREVIEW_COLOR = new Vector3f(0.58f, 0.18f, 0.85f);
    /** Purple trace while falling (shadow_slash-style TraceParticleOptions) */
    private static final Vector3f FALL_TRACE_COLOR = new Vector3f(0.65f, 0.22f, 0.92f);
    private static final double FALL_TRACE_DISK_RADIUS = 5.0;
    private static final int FALL_TRACE_PARTICLE_COUNT = 6;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(8)
            .build();

    public GravitationalCollapseSpell() {
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 5;
        this.castTime = CAST_TICKS;
        this.baseManaCost = 0;
        this.manaCostPerLevel = 0;
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
    public Vector3f getTargetingColor() {
        return PREVIEW_COLOR;
    }

    @Override
    public int getEffectiveCastTime(int spellLevel, @Nullable LivingEntity entity) {
        return getCastTime(spellLevel);
    }

    @Override
    public boolean canBeInterrupted(Player player) {
        return false;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        float dmg = getSpellPower(spellLevel, caster);
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(dmg, 2)),
                Component.translatable("spell.heroic_spirit_spell.gravitational_collapse.info_radius", (int) LANDING_RADIUS),
                Component.translatable("spell.heroic_spirit_spell.gravitational_collapse.info_range", MAX_SELECT_DISTANCE)
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.ENDER_CAST.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_LONG_CAST;
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData magicData) {
        if (level.isClientSide) {
            return true;
        }
        Vec3 groundHit = raycastGroundHitClamped(level, caster, MAX_SELECT_DISTANCE);
        if (groundHit == null) {
            if (caster instanceof ServerPlayer sp) {
                sp.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("spell.heroic_spirit_spell.gravitational_collapse.no_target")
                                .withStyle(ChatFormatting.RED)));
            }
            return false;
        }
        Vec3 displayPos = elevateTargetArea(groundHit);
        int color = Utils.packRGB(PREVIEW_COLOR);
        if (magicData.getAdditionalCastData() instanceof TargetAreaCastData tac) {
            TargetedAreaEntity area = tac.getCastingEntity();
            if (area != null && !area.isRemoved()) {
                area.setPos(displayPos);
                return true;
            }
        }
        TargetedAreaEntity area = TargetedAreaEntity.createTargetAreaEntity(level, displayPos, PREVIEW_RADIUS, color);
        area.setDuration(CAST_TICKS + 30);
        area.setShouldFade(true);
        magicData.setAdditionalCastData(new TargetAreaCastData(displayPos, area));
        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData magicData) {
        if (level.isClientSide) {
            return;
        }
        Vec3 displayAnchor;
        if (magicData.getAdditionalCastData() instanceof TargetAreaCastData tac) {
            TargetedAreaEntity ae = tac.getCastingEntity();
            displayAnchor = ae != null && !ae.isRemoved() ? ae.position() : tac.getCenter();
        } else {
            super.onCast(level, spellLevel, entity, castSource, magicData);
            return;
        }
        double tx = displayAnchor.x;
        double tz = displayAnchor.z;
        double preferredFeetY = displayAnchor.y + ABOVE_TARGET_BLOCKS;
        int now = level.getServer().getTickCount();

        ServerLevel serverLevel = (ServerLevel) level;
        // Must snapshot pull targets before moving the caster; list uses caster position() (cage is around pre-teleport feet).
        List<LivingEntity> cagePull = GravityCageRuntime.hasCageActiveHere(serverLevel, entity.getUUID())
                ? GravityCageRuntime.listNonAlliesNearCasterWhenCageActiveForCollapse(serverLevel, entity)
                : List.of();

        double casterFeetY = resolveSafeTeleportFeetY(serverLevel, entity, tx, tz, preferredFeetY, displayAnchor.y);
        teleportToCollapseAir(entity, serverLevel, tx, casterFeetY, tz);
        applyCollapseAirBuffs(entity, now);

        for (LivingEntity passenger : cagePull) {
            int idx = passenger.getId() % 7;
            double spread = 0.32;
            double ox = ((idx % 3) - 1) * spread;
            double oz = ((idx / 3) - 1) * spread;
            double px = tx + ox;
            double pz = tz + oz;
            double passengerFeetY = resolveSafeTeleportFeetY(serverLevel, passenger, px, pz, preferredFeetY, displayAnchor.y);
            teleportToCollapseAir(passenger, serverLevel, px, passengerFeetY, pz);
            applyCollapseAirBuffs(passenger, now);
        }

        int expire = level.getServer().getTickCount() + PENDING_LANDING_MAX_TICKS;
        GravitationalCollapsePending.arm(entity.getUUID(), spellLevel, expire);
        super.onCast(level, spellLevel, entity, castSource, magicData);
    }

    private static void teleportToCollapseAir(LivingEntity entity, ServerLevel level, double tx, double ty, double tz) {
        if (entity instanceof ServerPlayer sp) {
            sp.teleportTo(level, tx, ty, tz, Set.<RelativeMovement>of(), entity.getYRot(), entity.getXRot());
        } else {
            entity.moveTo(tx, ty, tz, entity.getYRot(), entity.getXRot());
        }
    }

    private static void applyCollapseAirBuffs(LivingEntity entity, int serverTick) {
        entity.addEffect(new MobEffectInstance(MobEffectRegistry.FALL_DAMAGE_IMMUNITY, FALL_IMMUNITY_TICKS, 0, false, false, true));
        entity.addEffect(new MobEffectInstance(MobEffectRegistry.ANTIGRAVITY, ANTIGRAVITY_TICKS, 0, false, true, true));
        GravitationalCollapseAirBuffs.armSpellGravityPhase(entity, serverTick + SPELL_GRAVITY_DELAY_TICKS);
    }

    /**
     * Server: purple downward trace while falling during pending collapse (caster only, called from events).
     */
    public static void spawnFallTraceParticles(ServerPlayer player) {
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }
        Vec3 down = new Vec3(0.0, -1.15, 0.0);
        double speed = 0.06;
        Vec3 center = player.getBoundingBox().getCenter();
        var rnd = level.random;
        for (int i = 0; i < FALL_TRACE_PARTICLE_COUNT; i++) {
            double theta = rnd.nextDouble() * Math.PI * 2;
            double u = rnd.nextDouble();
            double rad = FALL_TRACE_DISK_RADIUS * Math.sqrt(u);
            double ox = Math.cos(theta) * rad;
            double oz = Math.sin(theta) * rad;
            Vec3 start = new Vec3(center.x + ox, center.y, center.z + oz);
            Vec3 end = start.add(down);
            MagicManager.spawnParticles(level,
                    new TraceParticleOptions(Utils.v3f(end), FALL_TRACE_COLOR),
                    start.x, start.y, start.z, 1, 0, 0, 0, speed, false);
        }
    }

    public static void executeLanding(ServerPlayer player, int spellLevel) {
        Level level = player.level();
        if (level.isClientSide) {
            return;
        }
        AbstractSpell spell = ModSpells.GRAVITATIONAL_COLLAPSE.get();
        float damage = spell.getSpellPower(spellLevel, player);
        Vec3 center = player.position();
        double r2 = LANDING_RADIUS * LANDING_RADIUS;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(LANDING_RADIUS, 3, LANDING_RADIUS))) {
            if (target == player || target.isAlliedTo(player)) {
                continue;
            }
            if (target.position().distanceToSqr(center) > r2) {
                continue;
            }
            DamageSources.applyDamage(target, damage, spell.getDamageSource(player));
            target.addEffect(new MobEffectInstance(ModEffects.SPELL_GRAVITY, ENEMY_SLOW_TICKS, ENEMY_GRAVITY_AMPLIFIER, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, ENEMY_SLOW_TICKS, ENEMY_SLOW_AMPLIFIER, false, true, true));
        }
        if (level instanceof ServerLevel sl) {
            BlockPos bpos = player.blockPosition();
            sl.sendParticles(
                    new BlockParticleOption(ParticleTypes.BLOCK, level.getBlockState(bpos)).setPos(bpos),
                    center.x, center.y, center.z,
                    40,
                    0.0D, 0.0D, 0.0D,
                    0.22 + 0.05 * spellLevel);
            radialTremor(level, center, Mth.floor(LANDING_RADIUS));
            level.playSound(
                    null,
                    center.x, center.y, center.z,
                    SoundRegistry.ELDRITCH_BLAST.get(),
                    player.getSoundSource(),
                    1.2f,
                    0.92f + level.random.nextFloat() * 0.12f);
        }
    }

    private static void radialTremor(Level level, Vec3 center, int radius) {
        int cx = Mth.floor(center.x);
        int cz = Mth.floor(center.z);
        double cy = center.y;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > radius * radius) {
                    continue;
                }
                Vec3 column = new Vec3(cx + dx + 0.5, cy + 2.0, cz + dz + 0.5);
                Vec3 ground = Utils.moveToRelativeGroundLevel(level, column, 12);
                BlockPos below = BlockPos.containing(ground).below();
                Utils.createTremorBlock(level, below, Utils.random.nextFloat() * 0.15f + 0.3f);
            }
        }
    }

    private static Vec3 elevateTargetArea(Vec3 ground) {
        return ground.add(0.0, TARGET_AREA_Y_OFFSET, 0.0);
    }

    /**
     * Slightly in front of a vertical wall hit so the downward probe starts in air (not inside the column).
     */
    private static Vec3 outwardFromVerticalBlockFace(BlockHitResult blockHit) {
        Vec3 hit = blockHit.getLocation();
        Direction face = blockHit.getDirection();
        if (face.getAxis().isHorizontal()) {
            return hit.add(face.getStepX() * 0.101, 0.0, face.getStepZ() * 0.101);
        }
        return hit;
    }

    /**
     * Upper bound for vertical ray start: stay near crosshair / feet, never far above (avoids snapping to pillar tops).
     */
    private static double groundSnapProbeTop(LivingEntity caster, double referenceY) {
        double eyeY = caster.getEyePosition(1f).y;
        return Math.min(eyeY + 0.35, referenceY + 2.25);
    }

    /**
     * Straight down from ({@code x}, {@code topY}, {@code z}) — no upward escape (unlike
     * {@link Utils#moveToRelativeGroundLevel(Level, Vec3, int)}), so tall solids don't steal the aim height.
     */
    private static Vec3 snapGroundBelowColumn(Level level, double x, double z, double topY) {
        Vec3 start = new Vec3(x, topY, z);
        Vec3 end = new Vec3(x, topY - 256.0, z);
        HitResult hr = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return hr.getLocation();
    }

    /**
     * Raycast then clamp horizontal distance from caster feet to {@code maxHoriz}; snap Y to ground below clamped XZ.
     */
    @Nullable
    private static Vec3 raycastGroundHitClamped(Level level, LivingEntity caster, double maxHoriz) {
        Vec3 start = caster.getEyePosition(1f);
        Vec3 look = caster.getViewVector(1f);
        double reach = 96.0;
        Vec3 end = start.add(look.scale(reach));
        HitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, caster));
        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) {
            return null;
        }
        Vec3 aimColumn = outwardFromVerticalBlockFace(blockHit);
        double probeTop = groundSnapProbeTop(caster, aimColumn.y);
        Vec3 rawGround = snapGroundBelowColumn(level, aimColumn.x, aimColumn.z, probeTop);

        Vec3 feet = caster.position();
        Vec3 xzTarget = clampHorizontalToCaster(feet, rawGround, maxHoriz);
        probeTop = groundSnapProbeTop(caster, xzTarget.y);
        Vec3 grounded = snapGroundBelowColumn(level, xzTarget.x, xzTarget.z, probeTop);

        Vec3 wallAdjusted = pullGroundTowardCasterIfWallAdjacent(level, caster, grounded);
        probeTop = groundSnapProbeTop(caster, wallAdjusted.y);
        Vec3 regrounded = snapGroundBelowColumn(level, wallAdjusted.x, wallAdjusted.z, probeTop);

        Vec3 clamped = clampHorizontalToCaster(feet, regrounded, maxHoriz);
        probeTop = groundSnapProbeTop(caster, clamped.y);
        return snapGroundBelowColumn(level, clamped.x, clamped.z, probeTop);
    }

    private static Vec3 clampHorizontalToCaster(Vec3 casterFeet, Vec3 targetGround, double maxHoriz) {
        Vec3 horiz = new Vec3(targetGround.x - casterFeet.x, 0.0, targetGround.z - casterFeet.z);
        double hDist = horiz.length();
        if (hDist <= maxHoriz || hDist < 1e-6) {
            return targetGround;
        }
        Vec3 dir = horiz.scale(1.0 / hDist);
        return new Vec3(casterFeet.x + dir.x * maxHoriz, targetGround.y, casterFeet.z + dir.z * maxHoriz);
    }

    private static boolean hasNonEmptyCollision(Level level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    /**
     * True when the ground column is inside a block or horizontally touches a solid wall (selection hugging terrain).
     */
    private static boolean shouldPullAwayFromWall(Level level, Vec3 groundHit) {
        BlockPos column = BlockPos.containing(groundHit.x, groundHit.y - 0.02, groundHit.z);
        if (hasNonEmptyCollision(level, column)) {
            return true;
        }
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (hasNonEmptyCollision(level, column.relative(dir))) {
                return true;
            }
        }
        return false;
    }

    private static Vec3 pullGroundTowardCasterIfWallAdjacent(Level level, LivingEntity caster, Vec3 groundHit) {
        if (!shouldPullAwayFromWall(level, groundHit)) {
            return groundHit;
        }
        Vec3 casterFeet = caster.position();
        Vec3 toward = new Vec3(casterFeet.x - groundHit.x, 0.0, casterFeet.z - groundHit.z);
        double len = toward.length();
        if (len < 1e-4) {
            return groundHit;
        }
        Vec3 dir = toward.scale(1.0 / len);
        return groundHit.add(dir.scale(WALL_AVOID_PULL_BLOCKS));
    }

    /**
     * True when the block column at (floor(x), floor(z)) has no collision from {@code floor(selectionCenterY)+1}
     * through {@code floor(selectionCenterY) + CLEAR_COLUMN_ABOVE_SELECTION_BLOCKS} (six blocks above ring center).
     */
    private static boolean isClearSixBlocksAboveSelectionCenter(Level level, double x, double z, double selectionCenterY) {
        int bx = Mth.floor(x);
        int bz = Mth.floor(z);
        int base = Mth.floor(selectionCenterY);
        for (int dy = 1; dy <= CLEAR_COLUMN_ABOVE_SELECTION_BLOCKS; dy++) {
            if (hasNonEmptyCollision(level, new BlockPos(bx, base + dy, bz))) {
                return false;
            }
        }
        return true;
    }

    /**
     * If the preferred feet position intersects blocks (e.g. ceiling over ring center), step down until the standing
     * bounding box is clear (also avoids clipping upward into solids).
     * <p>
     * When the six block spaces above the selection center column are empty, only {@code preferredFeetY} needs a
     * collision check (no further upward scanning). If that column has any obstruction within those six layers,
     * {@code preferredFeetY} is skipped and search steps downward only.
     */
    private static double resolveSafeTeleportFeetY(
            ServerLevel level, LivingEntity entity, double x, double z, double preferredFeetY, double selectionCenterY) {
        EntityDimensions dim = entity.getDimensions(Pose.STANDING);
        AABB preferredBox = dim.makeBoundingBox(new Vec3(x, preferredFeetY, z)).deflate(1.0e-7);

        boolean columnClearAboveCenter = isClearSixBlocksAboveSelectionCenter(level, x, z, selectionCenterY);
        if (columnClearAboveCenter && level.noCollision(entity, preferredBox)) {
            return preferredFeetY;
        }

        double minY = preferredFeetY - TELEPORT_Y_SEARCH_DOWN;
        // Six layers above center obstructed: skip redundant sample at preferredFeetY (likely head in ceiling).
        // If column is obstructed but preferredFeetY still clears the BB, search must include preferredFeetY.
        double startY = preferredFeetY - TELEPORT_Y_STEP;
        if (!columnClearAboveCenter && level.noCollision(entity, preferredBox)) {
            startY = preferredFeetY;
        }
        for (double y = startY; y >= minY; y -= TELEPORT_Y_STEP) {
            AABB box = dim.makeBoundingBox(new Vec3(x, y, z)).deflate(1.0e-7);
            if (level.noCollision(entity, box)) {
                return y;
            }
        }
        return preferredFeetY;
    }
}
