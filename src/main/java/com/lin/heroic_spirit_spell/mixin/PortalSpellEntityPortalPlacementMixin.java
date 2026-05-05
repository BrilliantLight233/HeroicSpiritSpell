package com.lin.heroic_spirit_spell.mixin;

import com.lin.heroic_spirit_spell.util.PortalPlacementAdjust;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.RecastInstance;
import io.redspace.ironsspellbooks.spells.ender.PortalSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * After {@link PortalSpell} computes entity-portal {@code portalLocation}, nudge spawn: 0.5 toward caster if beside a
 * vertical wall; +0.5 Y when the ray hit the top of a block (floor); ensure at least 0.5 above the column support at
 * {@code BlockPos.containing(x, y - 0.1, z)}.
 */
@Mixin(value = PortalSpell.class, remap = false)
public class PortalSpellEntityPortalPlacementMixin {

    /**
     * First {@link Vec3} STORE is {@code hitResultPos}; second is {@code portalLocation} (see {@code handleEntityPortal}).
     */
    @ModifyVariable(
            method = "handleEntityPortal",
            at = @At(value = "STORE"),
            ordinal = 1,
            remap = false)
    private Vec3 heroicSpiritSpell$adjustEntityPortalLocation(
            Vec3 portalLocation,
            @Nullable RecastInstance recastInstance,
            Level level,
            int spellLevel,
            LivingEntity entity,
            CastSource castSource,
            MagicData playerMagicData,
            Player player,
            ServerLevel serverLevel,
            BlockHitResult blockHitResult) {
        return PortalPlacementAdjust.adjustPortalSpawn(level, entity, blockHitResult, portalLocation);
    }
}
