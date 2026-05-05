package com.lin.heroic_spirit_spell.mixin;

import io.redspace.ironsspellbooks.capabilities.magic.CooldownInstance;
import io.redspace.ironsspellbooks.capabilities.magic.PlayerCooldowns;
import io.redspace.ironsspellbooks.network.casting.SyncCooldownsPacket;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = PlayerCooldowns.class, remap = false)
public abstract class PlayerCooldownsSyncSnapshotMixin {

    @Shadow
    private Map<String, CooldownInstance> spellCooldowns;

    @Inject(method = "syncToPlayer", at = @At("HEAD"), cancellable = true, remap = false)
    private void heroicSpiritSpell$syncWithSnapshot(ServerPlayer serverPlayer, CallbackInfo ci) {
        Map<String, CooldownInstance> snapshot = new HashMap<>(this.spellCooldowns.size());
        this.spellCooldowns.forEach((spellId, cooldown) ->
                snapshot.put(spellId, new CooldownInstance(cooldown.getSpellCooldown(), cooldown.getCooldownRemaining())));
        PacketDistributor.sendToPlayer(serverPlayer, new SyncCooldownsPacket(snapshot));
        ci.cancel();
    }
}
