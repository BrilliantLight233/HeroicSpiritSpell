package com.lin.heroic_spirit_spell.network;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.client.GravityCageClientState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record GravityCageTrapPayload(boolean trapped) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<GravityCageTrapPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(HeroicSpiritSpell.MODID, "gravity_cage_trap"));

    public static final StreamCodec<RegistryFriendlyByteBuf, GravityCageTrapPayload> STREAM_CODEC =
            StreamCodec.composite(ByteBufCodecs.BOOL, GravityCageTrapPayload::trapped, GravityCageTrapPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GravityCageTrapPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> GravityCageClientState.setTrappedInEnemyCage(payload.trapped()));
    }
}
