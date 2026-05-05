package com.lin.heroic_spirit_spell.network;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.util.TargetAreaVisualOffset;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.UUID;

/**
 * Dedicated clients do not run {@link com.lin.heroic_spirit_spell.spells.ender.GravityCageSpell#onCast} logic;
 * they must still mark the ring UUID so {@link com.lin.heroic_spirit_spell.mixin.TargetedAreaEntityMixin} applies Y+1 follow.
 */
public record GravityCageRingYOffsetPayload(UUID ringEntityUuid) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<GravityCageRingYOffsetPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(HeroicSpiritSpell.MODID, "gravity_cage_ring_y"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GravityCageRingYOffsetPayload> STREAM_CODEC = StreamCodec.of(
            (RegistryFriendlyByteBuf buf, GravityCageRingYOffsetPayload p) -> buf.writeUUID(p.ringEntityUuid()),
            buf -> new GravityCageRingYOffsetPayload(buf.readUUID()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GravityCageRingYOffsetPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> TargetAreaVisualOffset.track(payload.ringEntityUuid()));
    }
}
