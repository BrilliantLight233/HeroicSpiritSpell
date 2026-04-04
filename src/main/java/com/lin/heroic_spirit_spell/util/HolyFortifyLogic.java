package com.lin.heroic_spirit_spell.util;

/**
 * 铁魔法神圣守护（Fortify）与黄心：效果开始时在原吸收上增加 {@code (amplifier + 1)}，
 * 等价档位取满足 {@code (a + 1) <= absorption} 的最大 {@code a}（无等级上限）。
 */
public final class HolyFortifyLogic {

    private HolyFortifyLogic() {
    }

    /**
     * 不超过当前黄心的最大等价 Amplifier（无上限）。
     */
    public static int equivalentAmplifierFromAbsorption(float absorption) {
        if (absorption <= 1.0e-4f) {
            return -1;
        }
        int a = (int) Math.floor(absorption + 1.0e-4f) - 1;
        return Math.max(-1, a);
    }

    /**
     * 将铁魔法「整档 + (amplifier+1)」修正为「仅增加与档位差相当」的黄心。
     */
    public static float correctedAbsorption(float absorptionBefore, int amplifier, int baseEquiv) {
        return Math.max(0.0f, absorptionBefore + (amplifier - baseEquiv));
    }
}
