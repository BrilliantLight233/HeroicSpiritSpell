package com.lin.heroic_spirit_spell.mixin;



import io.redspace.ironsspellbooks.api.magic.MagicData;

import io.redspace.ironsspellbooks.spells.holy.HasteSpell;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;

import io.redspace.ironsspellbooks.api.util.Utils;

import net.minecraft.world.entity.LivingEntity;

import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;

import org.spongepowered.asm.mixin.injection.At;

import org.spongepowered.asm.mixin.injection.Constant;

import org.spongepowered.asm.mixin.injection.ModifyConstant;

import org.spongepowered.asm.mixin.injection.Redirect;



import java.util.function.Predicate;



/**

 * 急迫：在 preCastTargetHelper 内用 Predicate 限制只能选中自身或盟友。

 * 命中敌人时辅助返回 false，原版会走「无有效目标」分支并写入 TargetEntityCastData(施法者)，可正常读条施法，

 * 且不会出现「史莱姆被选定」与敌方脚下光圈（该文案与同步包在命中合法目标后才发出）。

 */

@Mixin(HasteSpell.class)

public abstract class HasteSpellMixin {

    private static final float HEROIC_SPIRIT_SPELL$HASTE_RADIUS = 8.0f;



    @ModifyConstant(method = "onCast", constant = @Constant(floatValue = 3.0f), remap = false)

    private float heroicSpiritSpell$expandHasteRadius(float originalRadius) {

        return HEROIC_SPIRIT_SPELL$HASTE_RADIUS;

    }

    @ModifyConstant(method = "checkPreCastConditions", constant = @Constant(floatValue = 3.0f), remap = false)
    private float heroicSpiritSpell$expandHastePreviewRadius(float originalRadius) {
        return HEROIC_SPIRIT_SPELL$HASTE_RADIUS;
    }



    @Redirect(

            method = "checkPreCastConditions",

            at = @At(value = "INVOKE",

                    target = "Lio/redspace/ironsspellbooks/api/util/Utils;preCastTargetHelper(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/magic/MagicData;Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;IFZ)Z"),

            remap = false)

    private static boolean heroicSpiritSpell$preCastOnlySelfOrAllies(

            Level level,

            LivingEntity caster,

            MagicData magicData,

            AbstractSpell spell,

            int range,

            float aimAssist,

            boolean flag) {

        Predicate<LivingEntity> selfOrAllies = candidate -> candidate == caster || candidate.isAlliedTo(caster);

        return Utils.preCastTargetHelper(level, caster, magicData, spell, range, aimAssist, flag, selfOrAllies);

    }

}


