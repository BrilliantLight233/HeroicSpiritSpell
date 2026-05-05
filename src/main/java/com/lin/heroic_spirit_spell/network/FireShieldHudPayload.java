package com.lin.heroic_spirit_spell.network;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.hud.FireShieldHudState;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record FireShieldHudPayload(boolean visible, int hpCenti, int maxHpCenti, int breakTicks) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<FireShieldHudPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(HeroicSpiritSpell.MODID, "fire_shield_hud"));

    public static final StreamCodec<RegistryFriendlyByteBuf, FireShieldHudPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, FireShieldHudPayload::visible,
            ByteBufCodecs.VAR_INT, FireShieldHudPayload::hpCenti,
            ByteBufCodecs.VAR_INT, FireShieldHudPayload::maxHpCenti,
            ByteBufCodecs.VAR_INT, FireShieldHudPayload::breakTicks,
            FireShieldHudPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FireShieldHudPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> FireShieldHudState.apply(payload));
    }
}
