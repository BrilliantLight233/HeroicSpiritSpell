package com.lin.heroic_spirit_spell.client.network;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.client.FireballExplosionClientEffects;
import com.lin.heroic_spirit_spell.network.MagicFireballFieryExplosionPayload;
import com.lin.heroic_spirit_spell.network.ThunderstruckCameraPayload;
import com.lin.heroic_spirit_spell.network.ThunderstruckStage2CastAnimPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Registers S2C payloads whose handlers reference client-only classes.
 * Dedicated servers register the same types with no-op handlers in {@link com.lin.heroic_spirit_spell.network.HeroicSpiritSpellNetwork}.
 */
@EventBusSubscriber(modid = HeroicSpiritSpell.MODID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HeroicSpiritSpellClientPayloadRegistration {

    private HeroicSpiritSpellClientPayloadRegistration() {
    }

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var reg = event.registrar(HeroicSpiritSpell.MODID);
        reg.playToClient(
                ThunderstruckCameraPayload.TYPE,
                ThunderstruckCameraPayload.STREAM_CODEC,
                ThunderstruckClientPayloadHandlers::handleCamera
        );
        reg.playToClient(
                ThunderstruckStage2CastAnimPayload.TYPE,
                ThunderstruckStage2CastAnimPayload.STREAM_CODEC,
                ThunderstruckClientPayloadHandlers::handleStage2Anim
        );
        reg.playToClient(
                MagicFireballFieryExplosionPayload.TYPE,
                MagicFireballFieryExplosionPayload.STREAM_CODEC,
                (payload, ctx) -> ctx.enqueueWork(() -> FireballExplosionClientEffects.play(payload.pos(), payload.radius()))
        );
    }
}
