package com.lin.heroic_spirit_spell.client;

import com.lin.heroic_spirit_spell.hud.FireShieldHudState;
import com.lin.heroic_spirit_spell.util.FireShieldHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class FireShieldHudRenderer {
    private static final int SUBTITLE_BAND_OFFSET_BELOW_CENTER = 68;
    private static final int ACTION_BAR_BASELINE_FROM_BOTTOM = 59;
    private static final int FALLBACK_GAP_ABOVE_ACTION_BAR = 14;

    private FireShieldHudRenderer() {
    }

    public static void render(GuiGraphics graphics) {
        if (!FireShieldHudState.visible()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }
        Font font = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();
        int subtitleY = screenH / 2 + SUBTITLE_BAND_OFFSET_BELOW_CENTER;
        int actionBarY = screenH - ACTION_BAR_BASELINE_FROM_BOTTOM;
        int y;
        if (subtitleY >= actionBarY - font.lineHeight - 4) {
            y = actionBarY - font.lineHeight - FALLBACK_GAP_ABOVE_ACTION_BAR;
        } else {
            y = (subtitleY + actionBarY) / 2 - font.lineHeight / 2;
        }
        y = Mth.clamp(y, 2, screenH - 70);
        Component line = FireShieldHelper.buildHudLine(
                FireShieldHudState.hpCenti(), FireShieldHudState.maxHpCenti(), FireShieldHudState.breakTicks());
        graphics.drawCenteredString(font, line, screenW / 2, y, 0xFFFFFF);
    }
}
