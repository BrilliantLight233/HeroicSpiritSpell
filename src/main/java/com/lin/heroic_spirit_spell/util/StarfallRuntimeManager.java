package com.lin.heroic_spirit_spell.util;

import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StarfallRuntimeManager {
    private static final Map<UUID, StarfallRuntimeState> RUNTIME = new HashMap<>();

    public static StarfallRuntimeState getOrCreate(LivingEntity caster, java.util.function.Supplier<StarfallRuntimeState> supplier) {
        return RUNTIME.computeIfAbsent(caster.getUUID(), k -> supplier.get());
    }

    public static StarfallRuntimeState remove(LivingEntity caster) {
        return RUNTIME.remove(caster.getUUID());
    }
}
