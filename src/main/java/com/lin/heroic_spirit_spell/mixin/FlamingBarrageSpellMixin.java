package com.lin.heroic_spirit_spell.mixin;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.spells.fire.FlamingBarrageSpell;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

/**
 * Flaming Barrage: single charge; fireball uses view direction (pitch+yaw), no extra spread.
 */
@Mixin(FlamingBarrageSpell.class)
public abstract class FlamingBarrageSpellMixin {

    private static final int HEROIC_SPIRIT_SPELL$SINGLE_RECAST = 1;
    /** Same speed scale as vanilla spell: normalized aim then length 0.5 passed into SmallMagicFireball#shoot */
    private static final float HEROIC_SPIRIT_SPELL$SHOOT_SPEED = 0.5f;

    @Inject(method = "getRecastCount", at = @At("HEAD"), cancellable = true, remap = false)
    private void heroicSpiritSpell$singleCast(int spellLevel, @Nullable LivingEntity entity, CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(HEROIC_SPIRIT_SPELL$SINGLE_RECAST);
        cir.cancel();
    }

    @ModifyArgs(
            method = "onCast",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/entity/spells/fireball/SmallMagicFireball;shoot(Lnet/minecraft/world/phys/Vec3;F)V"),
            remap = false)
    private void heroicSpiritSpell$shootAlongLook(
            Args args,
            Level level,
            int spellLevel,
            LivingEntity entity,
            CastSource castSource,
            MagicData playerMagicData) {
        Vec3 look = entity.getViewVector(1.0f);
        args.set(0, look.scale(HEROIC_SPIRIT_SPELL$SHOOT_SPEED));
        args.set(1, 0.0f);
    }
}
