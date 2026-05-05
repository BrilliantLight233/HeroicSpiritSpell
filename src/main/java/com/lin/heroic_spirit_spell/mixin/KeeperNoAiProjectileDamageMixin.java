package com.lin.heroic_spirit_spell.mixin;

import io.redspace.ironsspellbooks.entity.mobs.keeper.KeeperEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(KeeperEntity.class)
public abstract class KeeperNoAiProjectileDamageMixin {
    /**
     * KeeperEntity#hurt applies a 0.75x projectile multiplier.
     * For NoAI keepers only, pre-scale to neutralize that reduction.
     */
    @ModifyVariable(method = "hurt", at = @At("HEAD"), ordinal = 0, argsOnly = true)
    private float heroicSpiritSpell$removeProjectileReductionForNoAi(float amount, DamageSource source) {
        if (((Mob) (Object) this).isNoAi() && source.getDirectEntity() instanceof Projectile) {
            return amount / 0.75f;
        }
        return amount;
    }
}
