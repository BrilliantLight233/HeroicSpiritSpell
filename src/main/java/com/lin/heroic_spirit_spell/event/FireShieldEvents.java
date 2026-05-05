package com.lin.heroic_spirit_spell.event;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.util.FireShieldHelper;
import com.lin.heroic_spirit_spell.util.FireShieldRuntime;
import io.redspace.ironsspellbooks.entity.spells.shield.ShieldEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = HeroicSpiritSpell.MODID)
public final class FireShieldEvents {
    private FireShieldEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer) || serverPlayer.level().isClientSide) {
            return;
        }
        int inscribedLevel = FireShieldRuntime.getInscribedSpellLevel(serverPlayer);
        if (inscribedLevel > 0 && FireShieldHelper.getStoredMaxHpCenti(serverPlayer) == 0) {
            FireShieldHelper.ensureCapacityForLevel(serverPlayer, inscribedLevel);
        }
        FireShieldHelper.applyScoreboardHpOverride(serverPlayer);
        FireShieldHelper.syncFireShieldHudToClient(serverPlayer, inscribedLevel > 0);

        if (FireShieldHelper.isFireShieldBarrierActive(serverPlayer)) {
            FireShieldHelper.ensureCapacityForLevel(serverPlayer, inscribedLevel);
            ShieldEntity shieldEntity = FireShieldHelper.getOrCreateShield(serverPlayer, inscribedLevel);
            FireShieldHelper.updateShieldAnchor(shieldEntity, serverPlayer);
            FireShieldHelper.applyStoredHpToShieldEntity(serverPlayer, shieldEntity);
            FireShieldHelper.syncShieldHealth(serverPlayer, shieldEntity);
        } else {
            FireShieldHelper.tickRecovery(serverPlayer);
            FireShieldHelper.clearShield(serverPlayer);
        }
    }
}
