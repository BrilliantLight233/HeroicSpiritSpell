package com.lin.heroic_spirit_spell.mixin;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.network.casting.CancelCastPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractSpell.class, remap = false)
public abstract class AbstractSpellQuickLongSwapMixin {

    @Unique
    private static final ThreadLocal<Boolean> HEROIC_SPIRIT_SPELL$REENTRY_GUARD = ThreadLocal.withInitial(() -> false);

    @Inject(method = "attemptInitiateCast", at = @At("HEAD"), cancellable = true, remap = false)
    private void heroicSpiritSpell$swapLongCastToNewSpell(
            ItemStack stack,
            int spellLevel,
            Level level,
            Player player,
            io.redspace.ironsspellbooks.api.spells.CastSource castSource,
            boolean triggerCooldown,
            String castingEquipmentSlot,
            CallbackInfoReturnable<Boolean> cir) {
        if (level.isClientSide() || HEROIC_SPIRIT_SPELL$REENTRY_GUARD.get() || !(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        AbstractSpell incoming = (AbstractSpell) (Object) this;
        MagicData magicData = MagicData.getPlayerMagicData(serverPlayer);
        if (!magicData.isCasting() || magicData.getCastType() != CastType.LONG) {
            return;
        }

        if (incoming.getSpellId().equals(magicData.getCastingSpellId())) {
            return;
        }

        // Allow immediate spell swap from an interrupted LONG cast without accidental cooldown.
        CancelCastPacket.cancelCast(serverPlayer, false);

        HEROIC_SPIRIT_SPELL$REENTRY_GUARD.set(true);
        try {
            boolean started = incoming.attemptInitiateCast(
                    stack,
                    spellLevel,
                    level,
                    player,
                    castSource,
                    triggerCooldown,
                    castingEquipmentSlot);
            cir.setReturnValue(started);
        } finally {
            HEROIC_SPIRIT_SPELL$REENTRY_GUARD.remove();
        }
    }
}
