package com.lin.heroic_spirit_spell.client.network;

import com.lin.heroic_spirit_spell.network.ThunderstruckCameraPayload;
import com.lin.heroic_spirit_spell.network.ThunderstruckStage2CastAnimPayload;
import com.lin.heroic_spirit_spell.util.ThunderstruckRuntime;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.render.animation.AnimationHelper;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** S2C payload handlers (client-only; must not be loaded on dedicated server). */
public final class ThunderstruckClientPayloadHandlers {

    private ThunderstruckClientPayloadHandlers() {
    }

    public static void handleCamera(ThunderstruckCameraPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.options == null) {
                return;
            }
            if (payload.rememberOriginalMode()) {
                boolean wasThird = mc.options.getCameraType() != CameraType.FIRST_PERSON;
                ThunderstruckRuntime.markOriginalCameraIsThirdPerson(mc.player, wasThird);
            }
            if (payload.switchToThirdPerson()) {
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
            if (payload.restoreFirstPersonIfNeeded()
                    && !ThunderstruckRuntime.wasOriginalCameraThirdPerson(mc.player)) {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            }
        });
    }

    public static void handleStage2Anim(ThunderstruckStage2CastAnimPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }
            SpellAnimations.ONE_HANDED_HORIZONTAL_SWING_ANIMATION
                    .getForPlayer()
                    .ifPresent(animation -> AnimationHelper.animatePlayerStart(mc.player, animation));
        });
    }
}
