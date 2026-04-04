package com.lin.heroic_spirit_spell.mixin;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.entity.spells.target_area.TargetedAreaEntity;
import com.lin.heroic_spirit_spell.util.HolyFortifyOverflow;
import io.redspace.ironsspellbooks.spells.TargetAreaCastData;
import io.redspace.ironsspellbooks.spells.holy.CleanseSpell;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * 净化：以射线锁定「自身或盟友」为中心生成光圈与生效范围；无合法目标时以施法者为中心。
 * 半径与急迫一致为 8；生效盒与原版比例一致（半径 8 → 每轴 16）。
 */
@Mixin(CleanseSpell.class)
public abstract class CleanseSpellMixin {

    private static final float HEROIC_SPIRIT_SPELL$CLEANSE_PREVIEW_RADIUS = 8.0f;
    private static final double HEROIC_SPIRIT_SPELL$CLEANSE_BOX_AXIS_SIZE = 16.0d;

    private static final ThreadLocal<MagicData> HEROIC_SPIRIT_SPELL$ON_CAST_MAGIC_DATA = new ThreadLocal<>();

    @Inject(method = "getCastType", at = @At("HEAD"), cancellable = true, remap = false)
    private void heroicSpiritSpell$makeCleanseInstant(CallbackInfoReturnable<CastType> cir) {
        cir.setReturnValue(CastType.INSTANT);
    }

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 60), remap = false)
    private int heroicSpiritSpell$instantCastTime(int original) {
        return 0;
    }

    @Inject(method = "onCast", at = @At("HEAD"), remap = false)
    private void heroicSpiritSpell$captureMagicDataForCenter(
            Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData magicData, CallbackInfo ci) {
        HEROIC_SPIRIT_SPELL$ON_CAST_MAGIC_DATA.set(magicData);
    }

    @Inject(method = "onCast", at = @At("RETURN"), remap = false)
    private void heroicSpiritSpell$clearMagicDataForCenter(CallbackInfo ci) {
        HEROIC_SPIRIT_SPELL$ON_CAST_MAGIC_DATA.remove();
    }

    /** 原版用施法者碰撞箱中心；改为与预施法一致的锁定中心（TargetAreaCastData） */
    @Redirect(
            method = "onCast",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/phys/AABB;getCenter()Lnet/minecraft/world/phys/Vec3;"),
            remap = false)
    private Vec3 heroicSpiritSpell$resolveCleanseAreaCenter(AABB aabb) {
        MagicData md = HEROIC_SPIRIT_SPELL$ON_CAST_MAGIC_DATA.get();
        if (md != null && md.getAdditionalCastData() instanceof TargetAreaCastData tac) {
            return tac.getCenter();
        }
        return aabb.getCenter();
    }

    @Inject(method = "checkPreCastConditions", at = @At("HEAD"), cancellable = true, remap = false)
    private void heroicSpiritSpell$cleanseLockTargetCenter(
            Level level, int spellLevel, LivingEntity caster, MagicData magicData, CallbackInfoReturnable<Boolean> cir) {
        AbstractSpell spell = (AbstractSpell) (Object) this;
        Predicate<LivingEntity> selfOrAllies = t -> t == caster || t.isAlliedTo(caster);
        boolean found = Utils.preCastTargetHelper(level, caster, magicData, spell, 32, 0.35f, false, selfOrAllies);

        LivingEntity locked = null;
        if (found && magicData.getAdditionalCastData() instanceof TargetEntityCastData ted) {
            locked = resolveLockedTarget(level, caster, ted);
        }
        Vec3 center = locked != null ? locked.position() : caster.position();
        /*
         * TargetedAreaEntity.tick 会把位置设为 owner.position()；必须传锁定目标为 owner，
         * 若始终传施法者则光圈会跳到玩家脚下（与 Haste 对目标传 owner 一致）。
         */
        Entity areaOwner = locked != null ? locked : caster;

        int color = Utils.packRGB(spell.getTargetingColor());
        TargetedAreaEntity area = TargetedAreaEntity.createTargetAreaEntity(
                level, center, HEROIC_SPIRIT_SPELL$CLEANSE_PREVIEW_RADIUS, color, areaOwner);
        magicData.setAdditionalCastData(new TargetAreaCastData(center, area));
        cir.setReturnValue(true);
        cir.cancel();
    }

    /** 解析射线锁定到的实体；客户端在范围内按 UUID 查找 */
    private static LivingEntity resolveLockedTarget(Level level, LivingEntity caster, TargetEntityCastData ted) {
        if (level instanceof ServerLevel serverLevel) {
            LivingEntity t = ted.getTarget(serverLevel);
            return t != null ? t : caster;
        }
        UUID uuid = ted.getTargetUUID();
        if (uuid == null) {
            return caster;
        }
        if (uuid.equals(caster.getUUID())) {
            return caster;
        }
        AABB search = caster.getBoundingBox().inflate(128.0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, search)) {
            if (uuid.equals(e.getUUID())) {
                return e;
            }
        }
        return caster;
    }

    @ModifyConstant(method = "onCast", constant = @Constant(doubleValue = 6.0d), remap = false)
    private double heroicSpiritSpell$expandCleanseEffectBox(double original) {
        return HEROIC_SPIRIT_SPELL$CLEANSE_BOX_AXIS_SIZE;
    }

    @ModifyConstant(method = "getUniqueInfo", constant = @Constant(intValue = 3), remap = false)
    private int heroicSpiritSpell$updateCleanseRadiusTooltip(int original) {
        return 8;
    }

    @Redirect(
            method = "lambda$onCast$1",
            at = @At(value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/util/Utils;shouldHealEntity(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z"),
            remap = false)
    private static boolean heroicSpiritSpell$cleanseOnlySelfOrAllies(LivingEntity caster, LivingEntity target) {
        boolean allowed = false;
        // 这里仅允许：自身 或 明确友方/同队（避免对非同队玩家生效）。
        if (target == caster) {
            allowed = true;
        } else {
            Team casterTeam = caster.getTeam();
            Team targetTeam = target.getTeam();
            // 原版 /team 语义：同队或 allied 的队伍才算友方
            if (casterTeam != null && targetTeam != null && (targetTeam == casterTeam || targetTeam.isAlliedTo(casterTeam))) {
                allowed = true;
            } else if (casterTeam != null && target.isAlliedTo(casterTeam)) {
                allowed = true;
            } else if (target.isAlliedTo(caster) || caster.isAlliedTo(target)) {
                allowed = true;
            }
        }
        if (allowed && !target.level().isClientSide) {
            heroicSpiritSpell$applyHealAndOverflowFortify(target);
        }
        return allowed;
    }

    /** 与 Wisp 命中队友一致：治疗 10，溢出转神圣守护；同等级不重复施加以免重置伤害吸收 */
    private static void heroicSpiritSpell$applyHealAndOverflowFortify(LivingEntity target) {
        float healAmount = 10.0f;
        float missingHealth = target.getMaxHealth() - target.getHealth();
        target.heal(healAmount);
        HolyFortifyOverflow.applyOverflowFortify(target, healAmount - missingHealth);
    }
}
