package com.lin.heroic_spirit_spell.event;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.spells.fire.SkyfireGuardMidflight;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = HeroicSpiritSpell.MODID)
public final class SkyfireGuardEvents {
    private SkyfireGuardEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof LivingEntity living) || living.level().isClientSide()) {
            return;
        }
        if (!living.getPersistentData().getBoolean(SkyfireGuardMidflight.ACTIVE_KEY)) {
            return;
        }
        if (!living.isAlive()) {
            SkyfireGuardMidflight.clear(living, true);
            return;
        }
        SkyfireGuardMidflight.tick(living);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp
                && sp.getPersistentData().getBoolean(SkyfireGuardMidflight.ACTIVE_KEY)) {
            SkyfireGuardMidflight.clear(sp, true);
        }
    }
}
