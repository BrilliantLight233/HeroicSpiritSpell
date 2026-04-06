package com.lin.heroic_spirit_spell.event;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.util.HolyShieldHelper;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = HeroicSpiritSpell.MODID)
public final class HolyShieldEvents {
    private HolyShieldEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer) || serverPlayer.level().isClientSide) {
            return;
        }
        if (!HolyShieldHelper.isHolyShieldSpellActive(serverPlayer)) {
            HolyShieldHelper.tickRecovery(serverPlayer);
        }
        HolyShieldHelper.syncScoreboard(serverPlayer);
    }
}
