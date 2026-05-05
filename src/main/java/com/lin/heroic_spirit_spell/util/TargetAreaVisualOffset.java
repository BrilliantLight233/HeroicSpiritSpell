package com.lin.heroic_spirit_spell.util;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Marks TargetedAreaEntity UUIDs that should render 1 block above owner (gravity cage). */
public final class TargetAreaVisualOffset {

    private static final Set<UUID> TRACKED = ConcurrentHashMap.newKeySet();

    private TargetAreaVisualOffset() {
    }

    public static void track(UUID areaEntityUuid) {
        TRACKED.add(areaEntityUuid);
    }

    public static void untrack(UUID areaEntityUuid) {
        TRACKED.remove(areaEntityUuid);
    }

    public static boolean isTracked(UUID areaEntityUuid) {
        return TRACKED.contains(areaEntityUuid);
    }
}
