package com.lin.heroic_spirit_spell.mixin.accessor;

import io.redspace.ironsspellbooks.spells.ender.StarfallSpell;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(StarfallSpell.StarfallCastData.class)
public interface StarfallCastDataAccessor {

    @Accessor("center")
    Vec3 hss_getCenter();

    @Accessor("center")
    void hss_setCenter(Vec3 center);

    @Accessor("trackedEntities")
    List<Entity> hss_getTrackedEntities();
}
