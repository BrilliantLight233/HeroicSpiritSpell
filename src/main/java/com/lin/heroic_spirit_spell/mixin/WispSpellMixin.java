package com.lin.heroic_spirit_spell.mixin;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.TargetEntityCastData;
import io.redspace.ironsspellbooks.entity.spells.wisp.WispEntity;
import io.redspace.ironsspellbooks.spells.holy.WispSpell;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WispSpell.class)
public abstract class WispSpellMixin {

    // 1. 强制 checkPreCastConditions 返回 true，允许无目标施法
    @Inject(method = "checkPreCastConditions", at = @At("RETURN"), cancellable = true, remap = false)
    private void heroicSpiritSpell$allowNoTargetCast(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, CallbackInfoReturnable<Boolean> cir) {
        // Utils.preCastTargetHelper 已经被调用过了（在原方法体中）。
        // 如果它找到了目标，MagicData 里会有数据。
        // 如果没找到，它返回 false，MagicData 里没数据。
        // 我们强制返回 true，这样即使没找到目标也能施法。
        cir.setReturnValue(true);
    }

    // 2. 处理无目标时的 Wisp 生成
    @Inject(method = "onCast", at = @At("HEAD"), remap = false)
    private void heroicSpiritSpell$onCastNoTarget(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        // 检查是否有目标数据
        if (!(playerMagicData.getAdditionalCastData() instanceof TargetEntityCastData)) {
            // 没有目标数据，说明是无锁定施法
            // 手动生成 Wisp
            
            // 获取法术强度
            AbstractSpell spell = (AbstractSpell) (Object) this;
            float power = spell.getSpellPower(spellLevel, entity);
            
            WispEntity wisp = new WispEntity(level, entity, power);
            
            // 不设置目标 (setTarget)，让 WispEntityMixin 中的 tick 逻辑去自动索敌
            
            // 设置位置：参考原版逻辑
            // Utils.getPositionFromEntityLookDirection(entity, 2.0f).subtract(0, 0.2, 0)
            wisp.setPos(Utils.getPositionFromEntityLookDirection(entity, 2.0f).subtract(0, 0.2, 0));
            
            level.addFreshEntity(wisp);
            
            // 原版 onCast 会因为 instanceof 检查失败而跳过生成逻辑，直接去调用 super.onCast
            // 所以这里不需要 cancel，让它继续执行即可触发 super.onCast (处理音效等)
        }
    }
}
