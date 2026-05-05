package com.lin.heroic_spirit_spell.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Tweaks Iron's Spellbooks {@code PortalSpell} world portal spawn position: nudge away from vertical walls, lift on
 * floor hits, and keep at least 0.5 blocks above the top of the support column at
 * {@code BlockPos.containing(x, y - 0.1, z)}.
 */
public final class PortalPlacementAdjust {

    public static final double WALL_OFFSET_TOWARD_CASTER = 0.5;
    public static final double FLOOR_UP_OFFSET = 0.5;

    private PortalPlacementAdjust() {
    }

    public static Vec3 adjustPortalSpawn(Level level, LivingEntity caster, BlockHitResult blockHit, Vec3 portalPos) {
        Vec3 v = portalPos;
        if (besideVerticalWall(level, v)) {
            v = pullHorizontalTowardCaster(v, caster, WALL_OFFSET_TOWARD_CASTER);
        }
        if (blockHit.getDirection() == Direction.UP) {
            v = v.add(0.0, FLOOR_UP_OFFSET, 0.0);
        }
        v = ensureMinHeightAboveColumnSupport(level, v);
        return v;
    }

    private static boolean hasBlockCollision(Level level, BlockPos pos) {
        return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    /** True if the column at the portal has a solid block on any horizontal side (vertical wall beside spawn). */
    private static boolean besideVerticalWall(Level level, Vec3 pos) {
        BlockPos column = BlockPos.containing(pos.x, pos.y - 0.1, pos.z);
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (hasBlockCollision(level, column.relative(dir))) {
                return true;
            }
        }
        return false;
    }

    private static Vec3 pullHorizontalTowardCaster(Vec3 pos, LivingEntity caster, double distance) {
        Vec3 feet = caster.position();
        Vec3 horiz = new Vec3(feet.x - pos.x, 0.0, feet.z - pos.z);
        double len = horiz.length();
        if (len < 1e-5) {
            return pos;
        }
        Vec3 dir = horiz.scale(1.0 / len);
        return pos.add(dir.x * distance, 0.0, dir.z * distance);
    }

    /**
     * Same column key as {@link #besideVerticalWall}: {@code BlockPos.containing(x, y - 0.1, z)}. Ray downward through
     * that column; if a collider is hit, keep the portal at least {@link #FLOOR_UP_OFFSET} above its collision top.
     */
    private static Vec3 ensureMinHeightAboveColumnSupport(Level level, Vec3 portalPos) {
        BlockPos columnKey = BlockPos.containing(portalPos.x, portalPos.y - 0.1, portalPos.z);
        double cx = columnKey.getX() + 0.5;
        double cz = columnKey.getZ() + 0.5;
        Vec3 from = new Vec3(cx, portalPos.y + 3.0, cz);
        Vec3 to = new Vec3(cx, level.getMinBuildHeight() - 1.0, cz);
        BlockHitResult hit =
                level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return portalPos;
        }
        BlockPos supportPos = hit.getBlockPos();
        var shape = level.getBlockState(supportPos).getCollisionShape(level, supportPos);
        if (shape.isEmpty()) {
            return portalPos;
        }
        double supportTopY = supportPos.getY() + shape.bounds().maxY;
        double minY = supportTopY + FLOOR_UP_OFFSET;
        if (portalPos.y < minY) {
            return new Vec3(portalPos.x, minY, portalPos.z);
        }
        return portalPos;
    }
}
