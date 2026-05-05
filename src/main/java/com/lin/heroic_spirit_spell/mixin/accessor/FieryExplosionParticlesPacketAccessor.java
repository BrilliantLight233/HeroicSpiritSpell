package com.lin.heroic_spirit_spell.mixin.accessor;

import io.redspace.ironsspellbooks.network.particles.FieryExplosionParticlesPacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FieryExplosionParticlesPacket.class)
public interface FieryExplosionParticlesPacketAccessor {

    @Accessor("pos1")
    Vec3 hss$getPos();

    @Accessor("radius")
    float hss$getRadius();
}
