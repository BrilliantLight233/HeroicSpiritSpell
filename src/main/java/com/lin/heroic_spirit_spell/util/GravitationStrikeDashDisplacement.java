package com.lin.heroic_spirit_spell.util;

import com.lin.heroic_spirit_spell.spells.ender.GravitationStrikeSpell;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

/**
 * 沿冲刺水平方向推动目标；使用 {@link LivingEntity#move} 小步位移以走原版碰撞，避免 {@code setPos} 穿进墙体。
 */
public final class GravitationStrikeDashDisplacement {

    private static final double KNOCKBACK_EXTRA_BLOCKS = 0.0;
    /** 单步水平位移（格），越小越不易穿模，步数略增 */
    private static final double DISPLACE_STEP = 0.2;
    private static final int MAX_STEPS = 64;

    private GravitationStrikeDashDisplacement() {
    }

    public static void snapTargetAlongDash(LivingEntity caster, LivingEntity target, int dashDamageAmplifier) {
        if (caster.level().isClientSide() || target == caster) {
            return;
        }
        Vec3 dir = GravitationStrikeSpell.getDashHorizontalDirection(caster);
        double knockDist = GravitationStrikeSpell.estimateHorizontalDashTravelBlocks(caster, dashDamageAmplifier) + KNOCKBACK_EXTRA_BLOCKS;
        if (knockDist <= 1e-4) {
            return;
        }
        double remaining = knockDist;
        for (int i = 0; i < MAX_STEPS && remaining > 1e-4; i++) {
            double seg = Math.min(DISPLACE_STEP, remaining);
            Vec3 delta = new Vec3(dir.x * seg, 0.0, dir.z * seg);
            double beforeX = target.getX();
            double beforeZ = target.getZ();
            target.move(MoverType.SELF, delta);
            double moved = Math.hypot(target.getX() - beforeX, target.getZ() - beforeZ);
            remaining -= moved;
            if (moved < seg * 0.35) {
                break;
            }
            if (target.horizontalCollision) {
                break;
            }
        }
        target.setDeltaMovement(0.0, target.getDeltaMovement().y, 0.0);
        target.hurtMarked = true;
    }
}
