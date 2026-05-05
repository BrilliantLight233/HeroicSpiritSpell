package com.lin.heroic_spirit_spell.mixin;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.LightningLanceCastHelper;
import com.lin.heroic_spirit_spell.util.LightningLanceDeferredRelease;
import com.lin.heroic_spirit_spell.util.LightningLanceSpawnGuard;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.network.casting.CancelCastPacket;
import io.redspace.ironsspellbooks.network.casting.OnCastFinishedPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Lightning lance: release before min charge -> defer; {@link LightningLanceDeferredRelease} auto-cancels at 0.5s charge.
 * <p>
 * When {@code rawElapsed==0}, {@code cancelCast} is rescheduled one tick (packet order vs {@code handleCastDuration}).
 * If ISS invokes {@code cancelCast} twice in the same tick, the second call must be swallowed or it runs
 * {@code onServerCastComplete} with zero charge and causes ghost HUD / no-shot.
 */
@Mixin(value = CancelCastPacket.class, remap = false)
public class CancelCastPacketMixin {

    private static final ThreadLocal<Boolean> HEROIC_SPIRIT_SPELL$ZERO_ELAPSED_RETRY = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> HEROIC_SPIRIT_SPELL$LONG_CANCEL_RETRY = ThreadLocal.withInitial(() -> false);
    private static final ThreadLocal<Boolean> HEROIC_SPIRIT_SPELL$EARLY_CANCEL_RETRY = ThreadLocal.withInitial(() -> false);

    @Inject(method = "cancelCast", at = @At("HEAD"), cancellable = true, remap = false)
    private static void heroicSpiritSpell$deferLightningLanceMinCharge(ServerPlayer serverPlayer, boolean triggerCooldown, CallbackInfo ci) {
        if (serverPlayer == null) {
            return;
        }
        MagicData md = MagicData.getPlayerMagicData(serverPlayer);
        int elapsed = md.getCastDuration() - md.getCastDurationRemaining();
        if (triggerCooldown
                && md.isCasting()
                && md.getCastType() == CastType.INSTANT
                && elapsed <= 1) {
            HeroicSpiritSpell.LOGGER.info(
                    "[HSS_FIX][CancelCast][IGNORE_EARLY_INSTANT_CANCEL] player={} spell={} elapsed={}",
                    serverPlayer.getGameProfile().getName(),
                    md.getCastingSpellId(),
                    elapsed
            );
            ci.cancel();
            return;
        }
        if (triggerCooldown && md.isCasting() && md.getCastType() == CastType.INSTANT) {
            String callerTrace = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                    .walk(stream -> stream
                            .skip(1)
                            .filter(frame -> {
                                String cls = frame.getClassName();
                                return !cls.contains("CancelCastPacketMixin")
                                        && !cls.contains("CancelCastPacket")
                                        && !cls.startsWith("java.")
                                        && !cls.startsWith("jdk.");
                            })
                            .limit(6)
                            .map(frame -> frame.getClassName() + "#" + frame.getMethodName() + ":" + frame.getLineNumber())
                            .collect(Collectors.joining(" <- ")));
            HeroicSpiritSpell.LOGGER.info(
                    "[HSS_PROBE_KEEP][CancelCast][INSTANT_TRIGGER_TRUE_CALLER] player={} spell={} elapsed={} trace={}",
                    serverPlayer.getGameProfile().getName(),
                    md.getCastingSpellId(),
                    elapsed,
                    callerTrace
            );
        }
        if (!HEROIC_SPIRIT_SPELL$EARLY_CANCEL_RETRY.get()
                && triggerCooldown
                && md.isCasting()) {
            if (elapsed <= 1) {
                HEROIC_SPIRIT_SPELL$EARLY_CANCEL_RETRY.set(true);
                try {
                    CancelCastPacket.cancelCast(serverPlayer, false);
                } finally {
                    HEROIC_SPIRIT_SPELL$EARLY_CANCEL_RETRY.remove();
                }
                ci.cancel();
                return;
            }
        }
        if (!HEROIC_SPIRIT_SPELL$LONG_CANCEL_RETRY.get()
                && triggerCooldown
                && md.isCasting()
                && md.getCastType() == CastType.LONG) {
            HEROIC_SPIRIT_SPELL$LONG_CANCEL_RETRY.set(true);
            try {
                CancelCastPacket.cancelCast(serverPlayer, false);
            } finally {
                HEROIC_SPIRIT_SPELL$LONG_CANCEL_RETRY.remove();
            }
            ci.cancel();
            return;
        }
        int chargedTicks = LightningLanceCastHelper.getChargedTicks(md);
        UUID playerId = serverPlayer.getUUID();
        long ticksSinceLastSpawn = LightningLanceSpawnGuard.ticksSinceLastSpawn(
                playerId,
                serverPlayer.level().getGameTime());
        if (!HEROIC_SPIRIT_SPELL$ZERO_ELAPSED_RETRY.get()
                && md.isCasting()
                && LightningLanceCastHelper.SPELL_ID.equals(md.getCastingSpellId())) {
            int rawElapsed = md.getCastDuration() - md.getCastDurationRemaining();
            if (rawElapsed <= 0) {
                if (!LightningLanceCastHelper.tryBeginZeroElapsedCancelCoalesce(playerId)) {
                    ci.cancel();
                    return;
                }
                MinecraftServer server = serverPlayer.getServer();
                if (server != null) {
                    server.execute(() -> {
                        LightningLanceCastHelper.clearZeroElapsedCancelCoalesce(playerId);
                        HEROIC_SPIRIT_SPELL$ZERO_ELAPSED_RETRY.set(true);
                        try {
                            CancelCastPacket.cancelCast(serverPlayer, triggerCooldown);
                        } finally {
                            HEROIC_SPIRIT_SPELL$ZERO_ELAPSED_RETRY.remove();
                        }
                    });
                    ci.cancel();
                    return;
                }
                LightningLanceCastHelper.clearZeroElapsedCancelCoalesce(playerId);
            }
        }
        // Same-tick "ghost" recast after a real release: charged reads 0 before first duration tick.
        if (LightningLanceCastHelper.SPELL_ID.equals(md.getCastingSpellId())
                && chargedTicks == 0
                && ticksSinceLastSpawn <= 1) {
            LightningLanceDeferredRelease.clear(playerId);
            String spellIdStr = md.getCastingSpellId();
            LightningLanceCastHelper.clearZeroElapsedCancelCoalesce(playerId);
            md.resetCastingState();
            String finishedSpellId = spellIdStr == null || spellIdStr.isEmpty()
                    ? LightningLanceCastHelper.SPELL_ID
                    : spellIdStr;
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                    serverPlayer,
                    new OnCastFinishedPacket(serverPlayer.getUUID(), finishedSpellId, true));
            ci.cancel();
            return;
        }
        if (LightningLanceCastHelper.shouldDeferCancel(md)) {
            LightningLanceDeferredRelease.schedule(serverPlayer, triggerCooldown);
            ci.cancel();
            return;
        }
        if (LightningLanceCastHelper.SPELL_ID.equals(md.getCastingSpellId())) {
            LightningLanceDeferredRelease.clear(playerId);
        }
    }
}
