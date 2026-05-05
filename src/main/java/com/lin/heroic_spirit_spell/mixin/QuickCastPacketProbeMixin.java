package com.lin.heroic_spirit_spell.mixin;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import io.redspace.ironsspellbooks.network.casting.QuickCastPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = QuickCastPacket.class, remap = false)
public abstract class QuickCastPacketProbeMixin {

    private static final boolean ENABLED = false;

    @Shadow
    private int slot;

    @Inject(method = "handle", at = @At("HEAD"), remap = false)
    private static void heroicSpiritSpell$probeQuickCastHandleHead(QuickCastPacket packet, IPayloadContext context, CallbackInfo ci) {
        if (!ENABLED) {
            return;
        }
        String playerName = context.player() instanceof ServerPlayer serverPlayer
                ? serverPlayer.getGameProfile().getName()
                : "<non-server-player>";
        HeroicSpiritSpell.LOGGER.info(
                "[HSS_PROBE2][QuickCast][HANDLE_HEAD] player={} slot={}",
                playerName,
                ((QuickCastPacketProbeMixin) (Object) packet).slot
        );
    }
}
