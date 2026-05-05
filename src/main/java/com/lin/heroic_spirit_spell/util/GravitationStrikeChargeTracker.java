package com.lin.heroic_spirit_spell.util;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Channel damage accumulator; dash phase applies stored amount on first successful hit. */
public final class GravitationStrikeChargeTracker {

    private static final String CASTING_SPELL_ID = HeroicSpiritSpell.MODID + ":gravitation_strike";

    private static final ConcurrentHashMap<UUID, Float> CHARGE_DAMAGE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Float> PENDING_DASH_RETRIBUTION = new ConcurrentHashMap<>();

    private GravitationStrikeChargeTracker() {
    }

    public static boolean isChannelingGravitationStrike(ServerPlayer player) {
        MagicData md = MagicData.getPlayerMagicData(player);
        return md.isCasting() && CASTING_SPELL_ID.equals(md.getCastingSpellId());
    }

    /** New cast: clear charge pool and any undashed retribution (no cross-cast stacking). */
    public static void resetForNewCast(ServerPlayer player) {
        UUID id = player.getUUID();
        CHARGE_DAMAGE.remove(id);
        PENDING_DASH_RETRIBUTION.remove(id);
    }

    public static void addChargeDamage(ServerPlayer player, float amount) {
        if (amount <= 0f) {
            return;
        }
        CHARGE_DAMAGE.merge(player.getUUID(), amount, Float::sum);
    }

    /** End channel: move accumulated damage into pending pool for first dash hit. */
    public static void transferChargeToDashRetribution(ServerPlayer player) {
        UUID id = player.getUUID();
        Float acc = CHARGE_DAMAGE.remove(id);
        float v = acc == null ? 0f : Math.max(0f, acc);
        if (v > 0f) {
            PENDING_DASH_RETRIBUTION.put(id, v);
        }
    }

    public static float peekPendingRetribution(ServerPlayer player) {
        Float a = PENDING_DASH_RETRIBUTION.get(player.getUUID());
        return a == null ? 0f : Math.max(0f, a);
    }

    public static void clearPendingRetribution(ServerPlayer player) {
        PENDING_DASH_RETRIBUTION.remove(player.getUUID());
    }
}
