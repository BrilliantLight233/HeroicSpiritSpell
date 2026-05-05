package com.lin.heroic_spirit_spell.event;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.util.MagmaTrembleRuntime;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = HeroicSpiritSpell.MODID)
public final class MagmaTrembleEvents {
    private MagmaTrembleEvents() {
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        MagmaTrembleRuntime.tickAll(event.getServer());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            MagmaTrembleRuntime.clear(player.getUUID());
        }
    }
}
