package com.lin.heroic_spirit_spell.mixin.client;

import com.lin.heroic_spirit_spell.LightningLanceCastHelper;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.gui.overlays.CastBarOverlay;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * ISS {@link CastBarOverlay}: CONTINUOUS spells show remaining time and inverted bar.
 * Lightning lance: show elapsed charge 0..max and fill bar with charge.
 */
@Mixin(value = CastBarOverlay.class, remap = false)
public class CastBarOverlayMixin {

    private static final String LIGHTNING_LANCE_SPELL_ID = "irons_spellbooks:lightning_lance";
    /** Same as CastBarOverlay.COMPLETION_BAR_WIDTH / IMAGE_WIDTH */
    private static final int HEROIC_SPIRIT_SPELL$CAST_BAR_COMPLETION_WIDTH = 44;
    private static final int HEROIC_SPIRIT_SPELL$CAST_BAR_IMAGE_WIDTH = 54;

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/api/util/Utils;timeFromTicks(FI)Ljava/lang/String;"),
            remap = false)
    private String heroicSpiritSpell$lightningLanceTimeString(float ticks, int precision) {
        if (heroicSpiritSpell$isLightningLanceContinuous()) {
            float elapsedTicks = ClientMagicData.getCastCompletionPercent() * ClientMagicData.getCastDuration();
            return Utils.timeFromTicks(Math.min(elapsedTicks, LightningLanceCastHelper.MAX_CHARGE_TICKS), precision);
        }
        return Utils.timeFromTicks(ticks, precision);
    }

    /**
     * Progress bar uses {@code blit(..., int width, int height)}; arg index 5 is width (0 = ResourceLocation).
     * Recompute width from real completion so we do not rely on fragile STORE ordinals.
     */
    @ModifyArg(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    remap = true,
                    target =
                            "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIII)V"),
            index = 5)
    private int heroicSpiritSpell$lightningLanceBarWidth(int width) {
        if (!heroicSpiritSpell$isLightningLanceContinuous()) {
            return width;
        }
        float elapsedTicks = ClientMagicData.getCastCompletionPercent() * ClientMagicData.getCastDuration();
        float completion = Math.min(1.0f, elapsedTicks / LightningLanceCastHelper.MAX_CHARGE_TICKS);
        int inset = (HEROIC_SPIRIT_SPELL$CAST_BAR_IMAGE_WIDTH - HEROIC_SPIRIT_SPELL$CAST_BAR_COMPLETION_WIDTH) / 2;
        return (int) (HEROIC_SPIRIT_SPELL$CAST_BAR_COMPLETION_WIDTH * completion + inset);
    }

    private static boolean heroicSpiritSpell$isLightningLanceContinuous() {
        return ClientMagicData.getCastType() == CastType.CONTINUOUS
                && LIGHTNING_LANCE_SPELL_ID.equals(ClientMagicData.getCastingSpellId());
    }
}
