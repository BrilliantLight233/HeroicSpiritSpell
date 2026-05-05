package com.lin.heroic_spirit_spell.event;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.registry.ModEffects;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = HeroicSpiritSpell.MODID)
public final class IncorporeityEvents {
    private static final String NOCLIP_MARK = HeroicSpiritSpell.MODID + ":incorporeity_noclip_mark";

    private IncorporeityEvents() {
    }

    @SubscribeEvent
    public static void onEntityTickPre(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!player.hasEffect(ModEffects.INCORPOREITY)) {
            return;
        }
        // Apply before vanilla movement/collision logic runs this tick.
        player.noPhysics = true;
        player.fallDistance = 0f;
        player.xxa = 0f;
        player.zza = 0f;
        player.setSprinting(false);
        player.getPersistentData().putBoolean(NOCLIP_MARK, true);
    }

    @SubscribeEvent
    public static void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        boolean active = player.hasEffect(ModEffects.INCORPOREITY);
        if (active) {
            player.noPhysics = true;
            player.fallDistance = 0f;
            player.xxa = 0f;
            player.zza = 0f;
            player.setSprinting(false);
            player.getPersistentData().putBoolean(NOCLIP_MARK, true);
            return;
        }

        if (player.getPersistentData().getBoolean(NOCLIP_MARK)) {
            player.getPersistentData().remove(NOCLIP_MARK);
            if (!player.isSpectator()) {
                player.noPhysics = false;
            }
        }
    }
}
