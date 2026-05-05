package com.lin.heroic_spirit_spell;

import io.redspace.ironsspellbooks.api.magic.MagicData;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Lightning lance charge range; shared by spell mixin, cancel packet, deferred release. */
public final class LightningLanceCastHelper {

    public static final String SPELL_ID = "irons_spellbooks:lightning_lance";
    /** Min charge 0.5s at 20 tps */
    public static final int MIN_CHARGE_TICKS = 10;
    public static final int MAX_CHARGE_TICKS = 20;
    /** Keep continuous cast alive so reaching full charge does not auto-finish/retrigger. */
    public static final int HOLD_CAST_TICKS = 72000;

    /**
     * ISS may call {@code cancelCast} twice in one tick (e.g. release edge). The first call defers
     * when {@code rawElapsed==0}; the second must not run the real cancel or it finishes as no-shot.
     */
    private static final ConcurrentHashMap<UUID, Boolean> ZERO_ELAPSED_CANCEL_PENDING = new ConcurrentHashMap<>();

    private LightningLanceCastHelper() {
    }

    /** @return true if this invocation owns the deferred retry slot; false = swallow as duplicate */
    public static boolean tryBeginZeroElapsedCancelCoalesce(UUID playerId) {
        return ZERO_ELAPSED_CANCEL_PENDING.putIfAbsent(playerId, Boolean.TRUE) == null;
    }

    public static void clearZeroElapsedCancelCoalesce(UUID playerId) {
        ZERO_ELAPSED_CANCEL_PENDING.remove(playerId);
    }

    public static int getChargedTicks(MagicData playerMagicData) {
        return Math.min(
                MAX_CHARGE_TICKS,
                Math.max(0, playerMagicData.getCastDuration() - playerMagicData.getCastDurationRemaining()));
    }

    /** Below min charge: defer cancel; {@link com.lin.heroic_spirit_spell.util.LightningLanceDeferredRelease} finishes at min. */
    public static boolean shouldDeferCancel(MagicData md) {
        int chargedTicks = getChargedTicks(md);
        return md.isCasting()
                && SPELL_ID.equals(md.getCastingSpellId())
                && chargedTicks > 0
                && chargedTicks < MIN_CHARGE_TICKS;
    }
}
