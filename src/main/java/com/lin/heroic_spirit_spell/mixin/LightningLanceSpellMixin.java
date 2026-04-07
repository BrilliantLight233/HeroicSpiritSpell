package com.lin.heroic_spirit_spell.mixin;

import com.lin.heroic_spirit_spell.LightningLanceChargeData;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.spells.lightning_lance.LightningLanceProjectile;
import io.redspace.ironsspellbooks.spells.lightning.LightningLanceSpell;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Mixin(value = LightningLanceSpell.class, remap = false)
public abstract class LightningLanceSpellMixin extends AbstractSpell {
    @Unique
    private static final int HEROIC_SPIRIT_SPELL$LIGHTNING_LANCE_MIN_CHARGE_TICKS = 10;
    @Unique
    private static final int HEROIC_SPIRIT_SPELL$LIGHTNING_LANCE_MAX_CHARGE_TICKS = 20;
    @Unique
    private static final float HEROIC_SPIRIT_SPELL$LIGHTNING_LANCE_MIN_DAMAGE_MULTIPLIER = 0.5f;
    @Unique
    private static final float HEROIC_SPIRIT_SPELL$LIGHTNING_LANCE_DAMAGE_MULTIPLIER_RANGE = 0.5f;

    @ModifyConstant(method = "<init>", constant = @Constant(intValue = 40), remap = false)
    private int heroicSpiritSpell$setLightningLanceMaxChargeTicks(int original) {
        return HEROIC_SPIRIT_SPELL$LIGHTNING_LANCE_MAX_CHARGE_TICKS;
    }

    @Inject(method = "getCastType", at = @At("HEAD"), cancellable = true, remap = false)
    private void heroicSpiritSpell$setContinuousCastType(CallbackInfoReturnable<CastType> cir) {
        cir.setReturnValue(CastType.CONTINUOUS);
    }
    /**
     * @author
     * @reason
     */
    @Inject(method = "onCast", at = @At("HEAD"), remap = false, cancellable = true)
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData, CallbackInfo ci) {
        ci.cancel();
    }
//    @Override
//    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
//
//        if (!(entity instanceof ServerPlayer serverPlayer) || playerMagicData == null) {
//            return;
//        }
//
//        int chargedTicks = heroicSpiritSpell$getChargedTicks(playerMagicData);
//        if (chargedTicks >= HEROIC_SPIRIT_SPELL$LIGHTNING_LANCE_MAX_CHARGE_TICKS
//                && !(playerMagicData.getAdditionalCastData() instanceof LightningLanceChargeData)) {
//            playerMagicData.setAdditionalCastData(new LightningLanceChargeData(heroicSpiritSpell$getDamageMultiplier(chargedTicks)));
//            Utils.serverSideCancelCast(serverPlayer, true);
//        }
//    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled) {

        if (!cancelled || !(entity instanceof ServerPlayer serverPlayer) || playerMagicData == null) {
            return;
        }

        float damageMultiplier;
        if (playerMagicData.getAdditionalCastData() instanceof LightningLanceChargeData(float multiplier)) {
            damageMultiplier = multiplier;
        } else {
            damageMultiplier = heroicSpiritSpell$getDamageMultiplier(heroicSpiritSpell$getChargedTicks(playerMagicData));
        }

        heroicSpiritSpell$spawnChargedLance(level, spellLevel, serverPlayer, damageMultiplier);

        playerMagicData.resetCastingState();
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(serverPlayer, new io.redspace.ironsspellbooks.network.casting.OnCastFinishedPacket(serverPlayer.getUUID(), getSpellId(), cancelled));

    }

    @Redirect(
            method = "onCast",
            at = @At(
                    value = "INVOKE",
                    target = "Lio/redspace/ironsspellbooks/entity/spells/lightning_lance/LightningLanceProjectile;setDamage(F)V"),
            remap = false)
    private void heroicSpiritSpell$scaleLightningLanceDamage(
            LightningLanceProjectile projectile,
            float damage,
            Level level,
            int spellLevel,
            LivingEntity entity,
            CastSource castSource,
            MagicData playerMagicData) {
        if (playerMagicData.getAdditionalCastData() instanceof LightningLanceChargeData chargeData) {
            projectile.setDamage(damage * chargeData.damageMultiplier());
        } else {
            projectile.setDamage(damage);
        }
    }

    @Unique
    private void heroicSpiritSpell$spawnChargedLance(Level level, int spellLevel, ServerPlayer caster, float damageMultiplier) {
        LightningLanceProjectile lance = new LightningLanceProjectile(level, caster);
        lance.setPos(caster.position().add(0, caster.getEyeHeight(), 0).add(caster.getForward()));
        lance.shoot(caster.getLookAngle());
        float baseDamage = ((AbstractSpell) (Object) this).getSpellPower(spellLevel, caster);
        lance.setDamage(baseDamage * damageMultiplier);
        level.addFreshEntity(lance);
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
        return HEROIC_SPIRIT_SPELL$LIGHTNING_LANCE_MIN_DAMAGE_MULTIPLIER
                + normalized * HEROIC_SPIRIT_SPELL$LIGHTNING_LANCE_DAMAGE_MULTIPLIER_RANGE;
    }


}
