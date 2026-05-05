package com.lin.heroic_spirit_spell.mixin;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellData;
import io.redspace.ironsspellbooks.network.casting.CancelCastPacket;
import io.redspace.ironsspellbooks.player.ServerPlayerEvents;
import io.redspace.ironsspellbooks.registries.ComponentRegistry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerPlayerEvents.class, remap = false)
public class ServerPlayerEventsQuickLongCastMixin {

    @Inject(method = "onUseItem", at = @At("HEAD"), cancellable = true, remap = false)
    private static void heroicSpiritSpell$allowRightClickSwitchFromQuickLongCast(PlayerInteractEvent.RightClickItem event, CallbackInfo ci) {
        var player = event.getEntity();
        if (player.level().isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        MagicData magicData = MagicData.getPlayerMagicData(serverPlayer);
        if (!magicData.isCasting() || magicData.getCastType() != CastType.LONG) {
            return;
        }

        // Quick-cast path in ISS uses ItemStack.EMPTY as casting item. Right-click with implement then gets blocked.
        ItemStack castingItem = magicData.getPlayerCastingItem();
        ItemStack usedItem = event.getItemStack();
        if (!castingItem.isEmpty() || !usedItem.has(ComponentRegistry.CASTING_IMPLEMENT)) {
            return;
        }

        SpellSelectionManager spellSelectionManager = new SpellSelectionManager(serverPlayer);
        SpellSelectionManager.SelectionOption selectionOption = spellSelectionManager.getSelection();
        if (selectionOption == null || selectionOption.spellData == SpellData.EMPTY) {
            return;
        }

        String castingSpellId = magicData.getCastingSpellId();
        String selectedSpellId = selectionOption.spellData.getSpell().getSpellId();
        if (selectedSpellId.equals(castingSpellId)) {
            event.setCancellationResult(InteractionResult.CONSUME);
            event.setCanceled(true);
            ci.cancel();
            return;
        }

        // Interrupt current long cast without cooldown, then start selected spell normally.
        CancelCastPacket.cancelCast(serverPlayer, false);

        int spellLevel = selectionOption.spellData.getSpell().getLevelFor(selectionOption.spellData.getLevel(), serverPlayer);
        String castingSlot = event.getHand().ordinal() == 0 ? SpellSelectionManager.MAINHAND : SpellSelectionManager.OFFHAND;
        boolean started = selectionOption.spellData.getSpell().attemptInitiateCast(
                usedItem,
                spellLevel,
                serverPlayer.level(),
                serverPlayer,
                selectionOption.getCastSource(),
                true,
                castingSlot);

        event.setCancellationResult(started ? InteractionResult.CONSUME : InteractionResult.FAIL);
        event.setCanceled(true);
        ci.cancel();
    }
}
