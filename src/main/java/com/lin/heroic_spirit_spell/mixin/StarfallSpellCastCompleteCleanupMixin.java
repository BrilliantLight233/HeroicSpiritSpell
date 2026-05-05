package com.lin.heroic_spirit_spell.mixin;

import com.lin.heroic_spirit_spell.util.StarfallRuntimeManager;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.network.casting.SyncTargetingDataPacket;
import io.redspace.ironsspellbooks.spells.ender.StarfallSpell;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = AbstractSpell.class, remap = false)
public class StarfallSpellCastCompleteCleanupMixin {

    @Inject(method = "onServerCastComplete", at = @At("HEAD"), remap = false)
    private void heroicSpiritSpell$cleanupStarfallRuntime(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled, CallbackInfo ci) {
        if (!((Object) this instanceof StarfallSpell)) {
            return;
        }
        var state = StarfallRuntimeManager.remove(entity);
        if (state != null && state.centerArea != null && !state.centerArea.isRemoved()) {
            state.centerArea.discard();
        }
        if (entity instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer, new SyncTargetingDataPacket(SpellRegistry.STARFALL_SPELL.get(), List.of()));
        }
    }
}
