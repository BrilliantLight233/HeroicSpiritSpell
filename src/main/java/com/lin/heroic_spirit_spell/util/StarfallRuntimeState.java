package com.lin.heroic_spirit_spell.util;

import io.redspace.ironsspellbooks.entity.spells.target_area.TargetedAreaEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class StarfallRuntimeState {
    public Vec3 smoothedCenter;
    @Nullable
    public UUID focusedTargetId;
    public float focusWeight;
    @Nullable
    public TargetedAreaEntity centerArea;
    @Nullable
    public UUID lastSyncedTargetId;

    public StarfallRuntimeState(Vec3 initialCenter) {
        this.smoothedCenter = initialCenter;
    }
}
