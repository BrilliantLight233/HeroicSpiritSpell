package com.lin.heroic_spirit_spell.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/**
 * ??????????????????????? {@link com.lin.heroic_spirit_spell.HeroicSpiritSpellClient} ?????
 */
public final class GravityCageTrapHud {

    private GravityCageTrapHud() {
    }

    public static void onRenderGuiPost(RenderGuiEvent.Post event) {
        if (!GravityCageClientState.isTrappedInEnemyCage()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) {
            return;
        }
        GuiGraphics g = event.getGuiGraphics();
        Component line = Component.translatable("message.heroic_spirit_spell.gravity_cage_trapped")
                .withStyle(ChatFormatting.RED);
        int w = mc.getWindow().getGuiScaledWidth();
        int sw = mc.font.width(line);
        int x = Mth.floor((w - sw) / 2.0f);
        int y = mc.getWindow().getGuiScaledHeight() - 55;
        g.drawString(mc.font, line, x, y, 0xFFFFFF, false);
    }
}
