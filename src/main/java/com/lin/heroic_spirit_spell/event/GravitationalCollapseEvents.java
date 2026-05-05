package com.lin.heroic_spirit_spell.event;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.spells.ender.GravitationalCollapseSpell;
import com.lin.heroic_spirit_spell.util.GravitationalCollapseAirBuffs;
import com.lin.heroic_spirit_spell.util.GravitationalCollapsePending;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

@EventBusSubscriber(modid = HeroicSpiritSpell.MODID)
public final class GravitationalCollapseEvents {

    private GravitationalCollapseEvents() {
    }

    @SubscribeEvent
    public static void onServerTickPost(ServerTickEvent.Post event) {
        GravitationalCollapseAirBuffs.tickDue(event.getServer());
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer sp) || sp.level().isClientSide()) {
            return;
        }
        GravitationalCollapsePending.LandingData data = GravitationalCollapsePending.get(sp.getUUID());
        if (data == null) {
            return;
        }
        int now = sp.level().getServer().getTickCount();
        if (now > data.expireGameTime()) {
            GravitationalCollapsePending.clear(sp.getUUID());
            return;
        }
        if (!sp.onGround()) {
            GravitationalCollapseSpell.spawnFallTraceParticles(sp);
            return;
        }
        GravitationalCollapseSpell.executeLanding(sp, data.spellLevel());
        GravitationalCollapsePending.clear(sp.getUUID());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        var id = event.getEntity().getUUID();
        GravitationalCollapsePending.clear(id);
        GravitationalCollapseAirBuffs.clear(id);
    }
}
