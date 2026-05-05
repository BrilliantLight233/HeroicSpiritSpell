package com.lin.heroic_spirit_spell.client;

import com.lin.heroic_spirit_spell.util.HolyShieldHelper;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Vanilla {@code blocking} is true only for {@link LivingEntity#getUseItem()}. When dual-wielding shields and
 * blocking with the main hand, the off-hand stack never matches, so the off-hand still uses the idle item model.
 * Re-register {@code blocking} for every shield-capable item so the inactive hand also selects the blocking model.
 * Relies on {@link DualShieldAnimationHelper#isDualShieldBlockVisualActive}, which aligns with item predicate onset
 * (not the 5-tick delay in {@link LivingEntity#isBlocking()}).
 */
public final class DualShieldItemProperties {
    private static final ResourceLocation BLOCKING = ResourceLocation.withDefaultNamespace("blocking");

    private DualShieldItemProperties() {}

    public static void registerBlockingPredicateForAllShields() {
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack probe = new ItemStack(item);
            if (!HolyShieldHelper.isShieldItem(probe)) {
                continue;
            }
            ItemProperties.register(item, BLOCKING, DualShieldItemProperties::shieldBlocking);
        }
    }

    private static float shieldBlocking(
            ItemStack stack, @Nullable ClientLevel level, @Nullable LivingEntity entity, int seed) {
        if (entity == null) {
            return 0f;
        }
        if (entity.isUsingItem() && stack == entity.getUseItem()) {
            return 1f;
        }
        if (!(entity instanceof Player player)) {
            return 0f;
        }
        if (!DualShieldAnimationHelper.isDualShieldBlockVisualActive(player)) {
            return 0f;
        }
        if (!HolyShieldHelper.isShieldItem(stack)) {
            return 0f;
        }
        ItemStack use = player.getUseItem();
        if (stack == use) {
            return 0f;
        }
        return stack == player.getMainHandItem() || stack == player.getOffhandItem() ? 1f : 0f;
    }
}
