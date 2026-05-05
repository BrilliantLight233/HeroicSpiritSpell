package com.lin.heroic_spirit_spell.client;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

@EventBusSubscriber(modid = HeroicSpiritSpell.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class FireShieldHudGui {
    private FireShieldHudGui() {
    }

    @SubscribeEvent
    public static void afterGui(RenderGuiEvent.Post event) {
        FireShieldHudRenderer.render(event.getGuiGraphics());
    }
}
