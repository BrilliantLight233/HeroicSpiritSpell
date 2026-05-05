package com.lin.heroic_spirit_spell.event;

import com.lin.heroic_spirit_spell.ElectrocuteCastHelper;
import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Electrocute: extra cooldown equals sustained channel time (capped at max cast), and record last channel ticks.
 */
@EventBusSubscriber(modid = HeroicSpiritSpell.MODID)
public final class ElectrocuteCooldownEvents {

    private ElectrocuteCooldownEvents() {
    }

    @SubscribeEvent
    public static void onSpellCooldownPre(SpellCooldownAddedEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer) || serverPlayer.level().isClientSide()) {
            return;
        }
        if (!ElectrocuteCastHelper.SPELL_ID.equals(event.getSpell().getSpellId())) {
            return;
        }
        MagicData md = MagicData.getPlayerMagicData(serverPlayer);
        if (!md.isCasting() || !ElectrocuteCastHelper.SPELL_ID.equals(md.getCastingSpellId())) {
            return;
        }
        int channelTicks = ElectrocuteCastHelper.getChannelTicksElapsed(md);
        int clamped = Math.min(channelTicks, ElectrocuteCastHelper.CAST_TIME_TICKS);
        ElectrocuteCastHelper.recordLastChannelTicks(serverPlayer.getUUID(), clamped);
        event.setEffectiveCooldown(event.getEffectiveCooldown() + clamped);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ElectrocuteCastHelper.clear(event.getEntity().getUUID());
    }
}
