package com.lin.heroic_spirit_spell.event;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.util.HolyShieldHelper;
import com.lin.heroic_spirit_spell.util.HolyShieldRuntime;
import io.redspace.ironsspellbooks.entity.spells.shield.ShieldEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = HeroicSpiritSpell.MODID)
public final class HolyShieldEvents {
    private HolyShieldEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer) || serverPlayer.level().isClientSide) {
            return;
        }
        int inscribedLevel = HolyShieldRuntime.getInscribedSpellLevel(serverPlayer);
        // Max/HP NBT 仅在举盾时写入；首次镌刻后先补满 max/hp，再同步 HUD，避免标题长期显示 0
        if (inscribedLevel > 0 && HolyShieldHelper.getStoredMaxHpCenti(serverPlayer) == 0) {
            HolyShieldHelper.ensureCapacityForLevel(serverPlayer, inscribedLevel);
        }
        HolyShieldHelper.applyScoreboardHpOverride(serverPlayer);
        HolyShieldHelper.syncHolyShieldHudToClient(serverPlayer, inscribedLevel > 0);

        if (HolyShieldHelper.isHolyShieldBarrierActive(serverPlayer)) {
            HolyShieldHelper.ensureCapacityForLevel(serverPlayer, inscribedLevel);
            ShieldEntity shieldEntity = HolyShieldHelper.getOrCreateShield(serverPlayer, inscribedLevel);
            HolyShieldHelper.updateShieldAnchor(shieldEntity, serverPlayer);
            HolyShieldHelper.applyStoredHpToShieldEntity(serverPlayer, shieldEntity);
            HolyShieldHelper.syncShieldHealth(serverPlayer, shieldEntity);
        } else {
            HolyShieldHelper.tickRecovery(serverPlayer);
            HolyShieldHelper.clearShield(serverPlayer);
        }
        HolyShieldHelper.syncScoreboard(serverPlayer);
    }
}
