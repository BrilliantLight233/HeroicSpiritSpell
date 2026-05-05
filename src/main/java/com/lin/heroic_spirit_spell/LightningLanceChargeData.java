package com.lin.heroic_spirit_spell;

import io.redspace.ironsspellbooks.api.spells.ICastData;

public record LightningLanceChargeData(float damageMultiplier) implements ICastData {
    @Override
    public void reset() {
    }
}