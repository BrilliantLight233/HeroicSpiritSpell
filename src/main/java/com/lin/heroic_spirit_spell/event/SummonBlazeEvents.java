package com.lin.heroic_spirit_spell.event;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.util.SummonBlazeRuntime;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = HeroicSpiritSpell.MODID)
public final class SummonBlazeEvents {

    private static final String FLAMING_BARRAGE_ID = "irons_spellbooks:flaming_barrage";
    private static final String BOOM_BARRAGE_ID = HeroicSpiritSpell.MODID + ":boom_barrage";

    private SummonBlazeEvents() {
    }

    @SubscribeEvent
    public static void onSpellCast(SpellOnCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        if (FLAMING_BARRAGE_ID.equals(event.getSpellId())) {
            SummonBlazeRuntime.onFlamingBarrageCast(player);
            return;
        }
        if (BOOM_BARRAGE_ID.equals(event.getSpellId())) {
            SummonBlazeRuntime.onBoomBarrageCast(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        if (!SummonBlazeRuntime.hasActive(player)) {
            return;
        }
        SummonBlazeRuntime.tickFollowAndExpiry(player);
        SummonBlazeRuntime.tickBarrage(player);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            SummonBlazeRuntime.clear(player);
        }
    }
}
