package com.lin.heroic_spirit_spell.util;

import com.lin.heroic_spirit_spell.LightningLanceCastHelper;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.casting.CancelCastPacket;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Early release before min charge: defer; when this player's charge reaches min, auto cancelCast.
 * Per-player pending only; processing runs in {@link com.lin.heroic_spirit_spell.event.LightningLanceEvents} on that player's tick (no global server tick scan).
 */
public final class LightningLanceDeferredRelease {

    private static final ConcurrentHashMap<UUID, Boolean> PENDING_TRIGGER_COOLDOWN = new ConcurrentHashMap<>();

    private LightningLanceDeferredRelease() {
    }

    public static void schedule(ServerPlayer player, boolean triggerCooldown) {
        PENDING_TRIGGER_COOLDOWN.put(player.getUUID(), triggerCooldown);
    }

    public static void clear(UUID playerId) {
        PENDING_TRIGGER_COOLDOWN.remove(playerId);
    }

    /** Call once per server tick per player (PlayerTickEvent); only acts if this uuid is pending. */
    public static void tryCompleteDeferredFor(ServerPlayer player) {
        UUID id = player.getUUID();
        Boolean triggerCooldown = PENDING_TRIGGER_COOLDOWN.get(id);
        if (triggerCooldown == null) {
            return;
        }
        MagicData md = MagicData.getPlayerMagicData(player);
        if (!md.isCasting() || !LightningLanceCastHelper.SPELL_ID.equals(md.getCastingSpellId())) {
            PENDING_TRIGGER_COOLDOWN.remove(id, triggerCooldown);
            return;
        }
        if (LightningLanceCastHelper.getChargedTicks(md) >= LightningLanceCastHelper.MIN_CHARGE_TICKS) {
            if (PENDING_TRIGGER_COOLDOWN.remove(id, triggerCooldown)) {
                CancelCastPacket.cancelCast(player, triggerCooldown);
            }
        }
    }
}
