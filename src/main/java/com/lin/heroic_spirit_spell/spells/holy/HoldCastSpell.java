package com.lin.heroic_spirit_spell.spells.holy;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

public abstract class HoldCastSpell extends AbstractSpell {
    protected HoldCastSpell(int castTimeTicks) {
        this.castTime = castTimeTicks;
    }

    @Override
    public CastType getCastType() {
        return CastType.CONTINUOUS;
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData) {
        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
        if (entity instanceof ServerPlayer serverPlayer && shouldRelease(serverPlayer, playerMagicData)) {
            Utils.serverSideCancelCast(serverPlayer, true);
        }
    }

    protected boolean shouldRelease(ServerPlayer serverPlayer, MagicData playerMagicData) {
        return false;
    }

    protected void cancelHold(ServerPlayer serverPlayer, boolean triggerCooldown) {
        Utils.serverSideCancelCast(serverPlayer, triggerCooldown);
    }
}
