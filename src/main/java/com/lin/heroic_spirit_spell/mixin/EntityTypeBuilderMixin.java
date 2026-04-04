package com.lin.heroic_spirit_spell.mixin;

import net.minecraft.world.entity.EntityType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

@Mixin(EntityType.Builder.class)
public class EntityTypeBuilderMixin {

    @Shadow
    private int updateInterval;

    @Shadow
    private int clientTrackingRange;

    private static final Set<String> HIGH_SPEED_PROJECTILES = Set.of(
            "irons_spellbooks:lightning_lance",
            "irons_spellbooks:magic_arrow",
            "irons_spellbooks:magic_missile",
            "irons_spellbooks:icicle",
            "irons_spellbooks:firebolt",
            "irons_spellbooks:fire_arrow",
            "irons_spellbooks:small_fireball",
            "irons_spellbooks:poison_arrow",
            "irons_spellbooks:guiding_bolt",
            "irons_spellbooks:blood_needle");

    @Inject(method = "build", at = @At("HEAD"))
    private void heroicSpiritSpell$modifyProjectileProperties(String id,
            CallbackInfoReturnable<EntityType<?>> cir) {
        if (HIGH_SPEED_PROJECTILES.contains(id)) {
            // Force high synchronization frequency (every tick)
            this.updateInterval = 1;
            // Increase tracking range
            this.clientTrackingRange = 128;
        }
    }
}
