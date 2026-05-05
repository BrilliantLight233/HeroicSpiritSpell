package com.lin.heroic_spirit_spell.event;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.util.GravitationStrikeChargeTracker;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = HeroicSpiritSpell.MODID)
public final class GravitationStrikeEvents {

    private GravitationStrikeEvents() {
    }

    @SubscribeEvent
    public static void onDamagePost(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        if (!GravitationStrikeChargeTracker.isChannelingGravitationStrike(player)) {
            return;
        }
        float dealt = event.getNewDamage();
        if (dealt <= 0f) {
            return;
        }
        GravitationStrikeChargeTracker.addChargeDamage(player, dealt);
    }
}
