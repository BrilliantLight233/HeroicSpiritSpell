package com.lin.heroic_spirit_spell.hud;

import com.lin.heroic_spirit_spell.network.FireShieldHudPayload;

/** Client-side cache for custom fire shield HUD (updated on main thread from S2C payload). */
public final class FireShieldHudState {

    private static volatile boolean visible;
    private static volatile int hpCenti;
    private static volatile int maxHpCenti;
    private static volatile int breakTicks;

    private FireShieldHudState() {
    }

    public static void apply(FireShieldHudPayload payload) {
        visible = payload.visible();
        hpCenti = payload.hpCenti();
        maxHpCenti = payload.maxHpCenti();
        breakTicks = payload.breakTicks();
    }

    public static boolean visible() {
        return visible;
    }

    public static int hpCenti() {
        return hpCenti;
    }

    public static int maxHpCenti() {
        return maxHpCenti;
    }

    public static int breakTicks() {
        return breakTicks;
    }
}
