package com.lin.heroic_spirit_spell.client;

import com.lin.heroic_spirit_spell.util.HolyShieldHelper;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.ItemAbilities;

import java.util.ArrayDeque;

/** Client-only: dual-shield raise animation (no gameplay changes). */
public final class DualShieldAnimationHelper {
    private record FirstPersonArmFrame(AbstractClientPlayer player, InteractionHand hand) {}

    private static final ThreadLocal<ArrayDeque<FirstPersonArmFrame>> FIRST_PERSON_ARM_STACK =
            ThreadLocal.withInitial(ArrayDeque::new);

    private DualShieldAnimationHelper() {}

    public static void beginFirstPersonArmRender(AbstractClientPlayer player, InteractionHand hand) {
        FIRST_PERSON_ARM_STACK.get().push(new FirstPersonArmFrame(player, hand));
    }

    public static void endFirstPersonArmRender() {
        ArrayDeque<FirstPersonArmFrame> stack = FIRST_PERSON_ARM_STACK.get();
        if (!stack.isEmpty()) {
            stack.pop();
        }
    }

    private static FirstPersonArmFrame firstPersonPeekFrame() {
        ArrayDeque<FirstPersonArmFrame> stack = FIRST_PERSON_ARM_STACK.get();
        return stack.isEmpty() ? null : stack.peek();
    }

    /**
     * True while the player is actively using a shield item and both hands hold shields.
     * <p>Uses the same onset as the vanilla {@code blocking} item predicate (first tick of
     * {@link LivingEntity#isUsingItem()} with shield use item), not {@link LivingEntity#isBlocking()} which waits 5
     * ticks before returning true. That way the off-hand matches the main hand visually.
     */
    public static boolean isDualShieldBlockVisualActive(Player player) {
        if (!(player instanceof LivingEntity living) || !living.isUsingItem()) {
            return false;
        }
        ItemStack use = living.getUseItem();
        if (use.isEmpty() || !use.canPerformAction(ItemAbilities.SHIELD_BLOCK)) {
            return false;
        }
        return HolyShieldHelper.isShieldItem(player.getMainHandItem())
                && HolyShieldHelper.isShieldItem(player.getOffhandItem());
    }

    /**
     * When NeoForge's {@code applyForgeHandTransform} returns true, vanilla skip-use-anim branch runs and the
     * off-hand never gets shield BLOCK transforms. For the inactive hand during dual-shield block, force vanilla path.
     */
    public static boolean shouldForceVanillaHandTransformInsteadOfForge(LocalPlayer forgePlayer) {
        if (!(forgePlayer instanceof AbstractClientPlayer acp)) {
            return false;
        }
        if (!isDualShieldBlockVisualActive(acp)) {
            return false;
        }
        FirstPersonArmFrame frame = firstPersonPeekFrame();
        if (frame == null || frame.player != acp) {
            return false;
        }
        return frame.hand != acp.getUsedItemHand();
    }

    /** Spoof {@link Player#getUsedItemHand()} for the opposite arm so vanilla applies the same BLOCK FP transform. */
    public static InteractionHand adjustUsedItemHandDuringFirstPersonArmRender(AbstractClientPlayer player) {
        InteractionHand real = player.getUsedItemHand();
        FirstPersonArmFrame frame = firstPersonPeekFrame();
        if (frame == null || frame.player != player || !isDualShieldBlockVisualActive(player)) {
            return real;
        }
        InteractionHand rendering = frame.hand;
        if (real == InteractionHand.MAIN_HAND && rendering == InteractionHand.OFF_HAND) {
            return InteractionHand.OFF_HAND;
        }
        if (real == InteractionHand.OFF_HAND && rendering == InteractionHand.MAIN_HAND) {
            return InteractionHand.MAIN_HAND;
        }
        return real;
    }
}
