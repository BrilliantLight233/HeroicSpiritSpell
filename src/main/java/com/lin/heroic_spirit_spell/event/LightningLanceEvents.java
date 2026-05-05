package com.lin.heroic_spirit_spell.event;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.LightningLanceCastHelper;
import com.lin.heroic_spirit_spell.util.LightningLanceDeferredRelease;
import com.lin.heroic_spirit_spell.util.LightningLanceSpawnGuard;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = HeroicSpiritSpell.MODID)
public final class LightningLanceEvents {

    private LightningLanceEvents() {
    }

    /** One player per tick: avoids multi-invocation ServerTick issues and cross-player coupling. */
    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        LightningLanceDeferredRelease.tryCompleteDeferredFor(player);
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        var id = event.getEntity().getUUID();
        LightningLanceDeferredRelease.clear(id);
        LightningLanceSpawnGuard.clear(id);
        LightningLanceCastHelper.clearZeroElapsedCancelCoalesce(id);
    }
}
