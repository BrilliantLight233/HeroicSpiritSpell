package com.lin.heroic_spirit_spell.mixin.accessor;

import net.minecraft.world.entity.projectile.Projectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import javax.annotation.Nullable;
import java.util.UUID;

@Mixin(Projectile.class)
public interface ProjectileOwnerAccessor {

    @Accessor("ownerUUID")
    @Nullable
    UUID getTrackedOwnerUuid();
}
