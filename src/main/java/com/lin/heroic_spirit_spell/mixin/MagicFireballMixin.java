package com.lin.heroic_spirit_spell.mixin;

import com.lin.heroic_spirit_spell.mixin.accessor.FieryExplosionParticlesPacketAccessor;
import com.lin.heroic_spirit_spell.network.MagicFireballFieryExplosionPayload;
import io.redspace.ironsspellbooks.entity.spells.fireball.MagicFireball;
import io.redspace.ironsspellbooks.network.particles.FieryExplosionParticlesPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(MagicFireball.class)
public class MagicFireballMixin {

    /** Trail loop count from {@code Mth.clamp((int) (vec3.lengthSqr() * 2), 1, 4)} scaled to 25%. */
    @Redirect(
            method = "trailParticles",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/Mth;clamp(III)I"))
    private int hss$scaleTrailSmokeClamp(int value, int min, int max) {
        int count = Mth.clamp(value, min, max);
        return Math.max(1, (int) Math.ceil(count * 0.25));
    }

    @Redirect(
            method = "onHit",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/network/PacketDistributor;sendToPlayersTrackingEntity(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;[Lnet/minecraft/network/protocol/common/custom/CustomPacketPayload;)V"))
    private void hss$sendReducedFierySmokeExplosion(
            Entity tracked, CustomPacketPayload payload, CustomPacketPayload[] extraPayloads) {
        if (payload instanceof FieryExplosionParticlesPacket p) {
            FieryExplosionParticlesPacketAccessor acc = (FieryExplosionParticlesPacketAccessor) (Object) p;
            PacketDistributor.sendToPlayersTrackingEntity(
                    tracked,
                    new MagicFireballFieryExplosionPayload(acc.hss$getPos(), acc.hss$getRadius()));
            return;
        }
        PacketDistributor.sendToPlayersTrackingEntity(tracked, payload, extraPayloads);
    }
}
