package com.lin.heroic_spirit_spell.mixin;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.network.casting.OnCastFinishedPacket;
import io.redspace.ironsspellbooks.spells.lightning.LightningLanceSpell;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import javax.annotation.Nullable;

@Mixin(LightningLanceSpell.class)
public abstract class LightningLanceSpellMixin {
    @Unique
    private static final int HEROIC_SPIRIT_SPELL$LIGHTNING_LANCE_CHARGE_TICKS = 40;
    @Unique
    private static final int HEROIC_SPIRIT_SPELL$LIGHTNING_LANCE_HOLD_TICKS = 20 * 60 * 60;

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 40), remap = false)
    private int heroicSpiritSpell$extendLightningLanceHoldTime(int original) {
        return HEROIC_SPIRIT_SPELL$LIGHTNING_LANCE_HOLD_TICKS;
    }

    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (!(entity instanceof ServerPlayer serverPlayer) || playerMagicData == null || serverPlayer.isUsingItem()) {
            return;
        }

        boolean fullyCharged = heroicSpiritSpell$isFullyCharged(playerMagicData);
        if (fullyCharged) {
            playerMagicData.setAdditionalCastData(LightningLanceReleaseData.READY);
        }
        Utils.serverSideCancelCast(serverPlayer, fullyCharged);
    }

    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled) {
        if (cancelled
                && entity instanceof ServerPlayer serverPlayer
                && playerMagicData.getAdditionalCastData() == LightningLanceReleaseData.READY) {
            ((AbstractSpell) (Object) this).castSpell(level, spellLevel, serverPlayer, playerMagicData.getCastSource(), false);
        }

        playerMagicData.resetCastingState();
        if (entity instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(
                    serverPlayer,
                    new OnCastFinishedPacket(serverPlayer.getUUID(), ((AbstractSpell) (Object) this).getSpellId(), cancelled));
        }
    }

    @Unique
    private static boolean heroicSpiritSpell$isFullyCharged(MagicData playerMagicData) {
        return playerMagicData.getCastDuration() - playerMagicData.getCastDurationRemaining()
                >= HEROIC_SPIRIT_SPELL$LIGHTNING_LANCE_CHARGE_TICKS;
    }

    @Unique
    private enum LightningLanceReleaseData implements ICastData {
        READY;

        @Override
        public void reset() {
        }
    }
}
