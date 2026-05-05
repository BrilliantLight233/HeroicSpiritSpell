package com.lin.heroic_spirit_spell.event;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.registry.ModEffects;
import com.lin.heroic_spirit_spell.util.EvasionStyleRandomTeleport;
import com.lin.heroic_spirit_spell.util.ExtremeSenseScheduler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;

import static net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Fatal check at LivingDamageEvent.Pre (post-armor, pre-absorption). Clears damage, applies Iron effects, delayed teleport.
 */
@EventBusSubscriber(modid = HeroicSpiritSpell.MODID)
public final class ExtremeSenseEvents {

    private static final int BUFFER_DURATION = 40;
    /** Same window as former abyssal (3s): Resistance V + delayed heartstop */
    private static final int RESISTANCE_BUFF_DURATION = BUFFER_DURATION + 20;
    /** Resistance level V (amplifier 4) */
    private static final int RESISTANCE_AMPLIFIER = 4;

    private ExtremeSenseEvents() {
    }

    @SubscribeEvent
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        if (!player.hasEffect(ModEffects.EXTREME_SENSE)) {
            return;
        }

        DamageSource source = event.getSource();
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return;
        }

        float dmg = event.getNewDamage();
        if (dmg <= 0f) {
            return;
        }

        float pool = player.getHealth() + player.getAbsorptionAmount();
        if (dmg < pool) {
            return;
        }

        event.setNewDamage(0f);

        player.removeEffect(ModEffects.EXTREME_SENSE);
        player.addEffect(new MobEffectInstance(
                DAMAGE_RESISTANCE, RESISTANCE_BUFF_DURATION, RESISTANCE_AMPLIFIER, false, false, true));
        // Heartstop + teleport: delayed by ExtremeSenseScheduler (+5 tick vs old instant / 40 tick teleport)

        EvasionStyleRandomTeleport.lookTowardSource(player, source);
        ExtremeSenseScheduler.arm(player);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer sp && !sp.level().isClientSide()) {
            ExtremeSenseScheduler.tick(sp);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        ExtremeSenseScheduler.onLogout(event.getEntity().getUUID());
    }
}
