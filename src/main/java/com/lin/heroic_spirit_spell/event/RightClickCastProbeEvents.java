package com.lin.heroic_spirit_spell.event;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Temporary probe for RightClickItem cast ordering conflicts in external modpacks.
 */
@EventBusSubscriber(modid = HeroicSpiritSpell.MODID)
public final class RightClickCastProbeEvents {

    private static final boolean ENABLED = false;

    private RightClickCastProbeEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST, receiveCanceled = true)
    public static void onRightClickItemHighest(PlayerInteractEvent.RightClickItem event) {
        if (ENABLED) {
            log("HIGHEST", event);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public static void onRightClickItemLowest(PlayerInteractEvent.RightClickItem event) {
        if (ENABLED) {
            log("LOWEST", event);
        }
    }

    private static void log(String phase, PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }

        MagicData magicData = MagicData.getPlayerMagicData(player);
        String castingSpellId = magicData.isCasting() ? magicData.getCastingSpellId() : "<none>";
        int elapsed = magicData.getCastDuration() - magicData.getCastDurationRemaining();

        SpellSelectionManager.SelectionOption selection = new SpellSelectionManager(player).getSelection();
        String selectedSpellId = "<empty>";
        if (selection != null && selection.spellData != SpellData.EMPTY) {
            selectedSpellId = selection.spellData.getSpell().getSpellId();
        }

        ItemStack stack = event.getItemStack();
        String itemId = stack.getItemHolder().unwrapKey()
                .map(k -> k.location().toString())
                .orElse(stack.getItem().toString());

        HeroicSpiritSpell.LOGGER.info(
                "[HSS_PROBE][RightClickItem][{}] player={} canceled={} result={} hand={} item={} isCasting={} castType={} castingSpell={} castElapsed={} selectedSpell={}",
                phase,
                player.getGameProfile().getName(),
                event.isCanceled(),
                event.getCancellationResult(),
                event.getHand(),
                itemId,
                magicData.isCasting(),
                magicData.getCastType(),
                castingSpellId,
                elapsed,
                selectedSpellId
        );
    }
}
