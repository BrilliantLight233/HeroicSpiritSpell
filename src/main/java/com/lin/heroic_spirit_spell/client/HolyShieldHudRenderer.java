package com.lin.heroic_spirit_spell.client;

import com.lin.heroic_spirit_spell.hud.HolyShieldHudState;
import com.lin.heroic_spirit_spell.util.HolyShieldHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class HolyShieldHudRenderer {

    /**
     * Approximate vertical center of vanilla {@code ClientboundSetSubtitleTextPacket} band
     * (below screen middle, matches Gui title stack layout in typical resolutions).
     */
    private static final int SUBTITLE_BAND_OFFSET_BELOW_CENTER = 68;
    /** Vanilla overlay / action bar message baseline: ~59px above bottom. */
    private static final int ACTION_BAR_BASELINE_FROM_BOTTOM = 59;
    /** When scaled GUI is too short, keep at least this gap above action bar baseline. */
    private static final int FALLBACK_GAP_ABOVE_ACTION_BAR = 14;

    private HolyShieldHudRenderer() {
    }

    public static void render(GuiGraphics graphics) {
        if (!HolyShieldHudState.visible()) {
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
        Component line = HolyShieldHelper.buildHudLine(
                HolyShieldHudState.hpCenti(), HolyShieldHudState.maxHpCenti(), HolyShieldHudState.breakTicks());
        graphics.drawCenteredString(font, line, screenW / 2, y, 0xFFFFFF);
    }
}
