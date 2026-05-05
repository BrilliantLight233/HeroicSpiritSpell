package com.lin.heroic_spirit_spell.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import java.util.Set;

@Mixin(Entity.class)
public abstract class EntityLerpMixin {

    private static final Set<String> HIGH_SPEED_PROJECTILE_CLASSES = Set.of(
            "io.redspace.ironsspellbooks.entity.spells.lightning_lance.LightningLanceProjectile",
            "io.redspace.ironsspellbooks.entity.spells.magic_arrow.MagicArrowProjectile",
            "io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile",
            "io.redspace.ironsspellbooks.entity.spells.icicle.IcicleProjectile",
            "io.redspace.ironsspellbooks.entity.spells.firebolt.FireboltProjectile",
            "io.redspace.ironsspellbooks.entity.spells.fire_arrow.FireArrowProjectile",
            "io.redspace.ironsspellbooks.entity.spells.poison_arrow.PoisonArrow",
            "io.redspace.ironsspellbooks.entity.spells.guiding_bolt.GuidingBoltProjectile",
            "io.redspace.ironsspellbooks.entity.spells.blood_needle.BloodNeedle");

    @ModifyVariable(method = "lerpTo(DDDFFI)V", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private int heroicSpiritSpell$ensureMinLerpSteps(int steps) {
        Object self = this;
        if (self != null && HIGH_SPEED_PROJECTILE_CLASSES.contains(self.getClass().getName())) {
            return Math.max(steps, 1);
        }
        return steps;
    }
}