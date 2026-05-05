package com.lin.heroic_spirit_spell.mixin.accessor;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Read/write vertical lerp fields so TargetedAreaEntity can stay +1 above owner without jitter. */
@Mixin(Entity.class)
public interface EntityLerpAccessor {

    @Accessor("yo")
    double hss_getYo();

    @Accessor("yo")
    void hss_setYo(double value);

    @Accessor("yOld")
    double hss_getYOld();

    @Accessor("yOld")
    void hss_setYOld(double value);
}
