package com.lin.heroic_spirit_spell;

import io.redspace.ironsspellbooks.api.magic.MagicData;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Electrocute: max channel length, elapsed channel readout, last release channel time (server). */
public final class ElectrocuteCastHelper {

    public static final String SPELL_ID = "irons_spellbooks:electrocute";
    /** 4s sustained cast cap at 20 tps */
    public static final int CAST_TIME_TICKS = 90;

    private static final ConcurrentHashMap<UUID, Integer> LAST_CHANNEL_TICKS = new ConcurrentHashMap<>();

    private ElectrocuteCastHelper() {
    }

    /** Elapsed continuous cast ticks while {@link MagicData} still reflects the active cast. */
    public static int getChannelTicksElapsed(MagicData playerMagicData) {
        int total = playerMagicData.getCastDuration();
        int remaining = playerMagicData.getCastDurationRemaining();
        return Math.max(0, total - remaining);
    }

    public static void recordLastChannelTicks(UUID playerId, int ticks) {
        LAST_CHANNEL_TICKS.put(playerId, ticks);
    }

    /** Last electrocute channel length (ticks) when cooldown was applied; absent if never or cleared. */
    public static int getLastRecordedChannelTicks(UUID playerId) {
        return LAST_CHANNEL_TICKS.getOrDefault(playerId, 0);
    }

    public static void clear(UUID playerId) {
        LAST_CHANNEL_TICKS.remove(playerId);
    }
}
