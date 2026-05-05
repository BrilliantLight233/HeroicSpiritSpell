package com.lin.heroic_spirit_spell.event;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.spells.fire.FireRiseAscensionDelay;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = HeroicSpiritSpell.MODID)
public final class FireRiseEvents {

    private FireRiseEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide()) {
            return;
        }
        if (!living.getPersistentData().contains(FireRiseAscensionDelay.PERSIST_KEY)) {
            return;
        }
        FireRiseAscensionDelay.tick(living);
    }
}
