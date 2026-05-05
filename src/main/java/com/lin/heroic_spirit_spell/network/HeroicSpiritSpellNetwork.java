package com.lin.heroic_spirit_spell.network;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = HeroicSpiritSpell.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class HeroicSpiritSpellNetwork {

    private HeroicSpiritSpellNetwork() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var reg = event.registrar(HeroicSpiritSpell.MODID);
        reg.playToClient(
                HolyShieldHudPayload.TYPE,
                HolyShieldHudPayload.STREAM_CODEC,
                HolyShieldHudPayload::handle
        );
        reg.playToClient(
                FireShieldHudPayload.TYPE,
                FireShieldHudPayload.STREAM_CODEC,
                FireShieldHudPayload::handle
        );
        reg.playToClient(
                GravityCageTrapPayload.TYPE,
                GravityCageTrapPayload.STREAM_CODEC,
                GravityCageTrapPayload::handle
        );
        reg.playToClient(
                GravityCageRingYOffsetPayload.TYPE,
                GravityCageRingYOffsetPayload.STREAM_CODEC,
                GravityCageRingYOffsetPayload::handle
        );
        // Handlers touch Minecraft / LocalPlayer — register no-op on dedicated server only;
        // real handlers: HeroicSpiritSpellClientPayloadRegistration (Dist.CLIENT).
        if (FMLEnvironment.dist == Dist.DEDICATED_SERVER) {
            reg.playToClient(
                    ThunderstruckCameraPayload.TYPE,
                    ThunderstruckCameraPayload.STREAM_CODEC,
                    (payload, ctx) -> {
                    }
            );
            reg.playToClient(
                    ThunderstruckStage2CastAnimPayload.TYPE,
                    ThunderstruckStage2CastAnimPayload.STREAM_CODEC,
                    (payload, ctx) -> {
                    }
            );
            reg.playToClient(
                    MagicFireballFieryExplosionPayload.TYPE,
                    MagicFireballFieryExplosionPayload.STREAM_CODEC,
                    (payload, ctx) -> {
                    }
            );
        }
    }
}
