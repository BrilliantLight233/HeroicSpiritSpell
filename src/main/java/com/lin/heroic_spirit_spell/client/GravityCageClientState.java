package com.lin.heroic_spirit_spell.client;

/** Server sync: local player is inside an enemy Gravity Cage (for subtitle HUD). */
public final class GravityCageClientState {

    private static volatile boolean trappedInEnemyCage;

    private GravityCageClientState() {
    }

    public static void setTrappedInEnemyCage(boolean value) {
        trappedInEnemyCage = value;
    }

    public static boolean isTrappedInEnemyCage() {
        return trappedInEnemyCage;
    }
}
