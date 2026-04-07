package com.lin.heroic_spirit_spell.mixin;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.spells.lightning.LightningLanceSpell;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(value = LightningLanceSpell.class, remap = false)
public abstract class LightningLanceSpellMixin {
    @Unique
    private static final int HEROIC_SPIRIT_SPELL$LIGHTNING_LANCE_MIN_CHARGE_TICKS = 10;
    @Unique
    private static final int HEROIC_SPIRIT_SPELL$LIGHTNING_LANCE_MAX_CHARGE_TICKS = 20;
    @Unique
    private static final int HEROIC_SPIRIT_SPELL$LIGHTNING_LANCE_HOLD_TICKS = 20 * 60 * 60;
    @Unique
    private static final ThreadLocal<LightningLanceChargeData> HEROIC_SPIRIT_SPELL$CHARGE_DATA = new ThreadLocal<>();

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 40), remap = false)
    private int heroicSpiritSpell$extendLightningLanceHoldTime(int original) {
        return HEROIC_SPIRIT_SPELL$LIGHTNING_LANCE_HOLD_TICKS;
    }

    @Inject(method = "onServerCastTick", at = @At("HEAD"), remap = false)
    private void heroicSpiritSpell$releaseLightningLanceOnKeyUp(
            Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData, CallbackInfo ci) {
        if (!(entity instanceof ServerPlayer serverPlayer) || playerMagicData == null || serverPlayer.isUsingItem()) {
            return;
        }

        int chargedTicks = heroicSpiritSpell$getChargedTicks(playerMagicData);
        if (chargedTicks < HEROIC_SPIRIT_SPELL$LIGHTNING_LANCE_MIN_CHARGE_TICKS) {
            playerMagicData.resetAdditionalCastData();
            Utils.serverSideCancelCast(serverPlayer, false);
            return;
        }

        playerMagicData.setAdditionalCastData(new LightningLanceChargeData(heroicSpiritSpell$getDamageMultiplier(chargedTicks)));
        Utils.serverSideCancelCast(serverPlayer, true);
    }

    @Inject(method = "onCast", at = @At("HEAD"), remap = false)
    private void heroicSpiritSpell$captureChargeData(
            Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        if (playerMagicData.getAdditionalCastData() instanceof LightningLanceChargeData chargeData) {
            HEROIC_SPIRIT_SPELL$CHARGE_DATA.set(chargeData);
        } else {
            HEROIC_SPIRIT_SPELL$CHARGE_DATA.remove();
        }
    }

    @Inject(method = "onServerCastComplete", at = @At("HEAD"), remap = false)
    private void heroicSpiritSpell$fireLightningLanceOnRelease(
            Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled, CallbackInfo ci) {
        if (cancelled
                && entity instanceof ServerPlayer serverPlayer
                && playerMagicData.getAdditionalCastData() instanceof LightningLanceChargeData) {
            ((AbstractSpell) (Object) this).castSpell(level, spellLevel, serverPlayer, playerMagicData.getCastSource(), false);
        }
    }

    @ModifyArg(
            method = "onCast",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/entity/spells/lightning_lance/LightningLanceProjectile;setDamage(F)V"),
            index = 0,
            remap = false)
    private float heroicSpiritSpell$scaleLightningLanceDamage(float damage) {
        LightningLanceChargeData chargeData = HEROIC_SPIRIT_SPELL$CHARGE_DATA.get();
        return chargeData != null ? damage * chargeData.damageMultiplier : damage;
    }

    @Inject(method = "onCast", at = @At("RETURN"), remap = false)
    private void heroicSpiritSpell$clearChargeData(CallbackInfo ci) {
        HEROIC_SPIRIT_SPELL$CHARGE_DATA.remove();
    }

    @Unique
    private static int heroicSpiritSpell$getChargedTicks(MagicData playerMagicData) {
        return Math.min(
                HEROIC_SPIRIT_SPELL$LIGHTNING_LANCE_MAX_CHARGE_TICKS,
                Math.max(0, playerMagicData.getCastDuration() - playerMagicData.getCastDurationRemaining()));
    }

    @Unique
    private static float heroicSpiritSpell$getDamageMultiplier(int chargedTicks) {
        int clampedTicks = Math.min(
                HEROIC_SPIRIT_SPELL$LIGHTNING_LANCE_MAX_CHARGE_TICKS,
                Math.max(HEROIC_SPIRIT_SPELL$LIGHTNING_LANCE_MIN_CHARGE_TICKS, chargedTicks));
        float normalized = (clampedTicks - HEROIC_SPIRIT_SPELL$LIGHTNING_LANCE_MIN_CHARGE_TICKS)
                / (float) (HEROIC_SPIRIT_SPELL$LIGHTNING_LANCE_MAX_CHARGE_TICKS - HEROIC_SPIRIT_SPELL$LIGHTNING_LANCE_MIN_CHARGE_TICKS);
        return 0.5f + normalized * 0.5f;
    }

    @Unique
    private static final class LightningLanceChargeData implements ICastData {
        private final float damageMultiplier;

        private LightningLanceChargeData(float damageMultiplier) {
            this.damageMultiplier = damageMultiplier;
        }

        @Override
        public void reset() {
        }
    }
}
