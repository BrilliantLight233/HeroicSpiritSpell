package com.lin.heroic_spirit_spell.client;

import com.lin.heroic_spirit_spell.registry.ModEffects;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;

/** Client-side hard input lock while incorporeity is active. */
public final class IncorporeityInputLock {
    private IncorporeityInputLock() {
    }

    public static void onMovementInputUpdate(MovementInputUpdateEvent event) {
        var player = event.getEntity();
        if (player == null || !player.hasEffect(ModEffects.INCORPOREITY)) {
            return;
        }
        var input = event.getInput();
        input.forwardImpulse = 0f;
        input.leftImpulse = 0f;
        input.up = false;
        input.down = false;
        input.left = false;
        input.right = false;
        input.jumping = false;
        input.shiftKeyDown = false;
    }
}
