package com.lin.heroic_spirit_spell.util;

import com.lin.heroic_spirit_spell.registry.ModSpells;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.SpellSelectionManager;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.server.level.ServerPlayer;

/**
 * Resolves inscribed Fire Shield level from spell wheel (learned + on a spell container).
 */
public final class FireShieldRuntime {
    private FireShieldRuntime() {
    }

    public static int getInscribedSpellLevel(ServerPlayer player) {
        AbstractSpell fireShield = ModSpells.FIRE_SHIELD.get();
        var synced = MagicData.getPlayerMagicData(player).getSyncedData();
        if (!synced.isSpellLearned(fireShield)) {
            return 0;
        }
        var manager = new SpellSelectionManager(player);
        for (SpellSelectionManager.SelectionOption option : manager.getAllSpells()) {
            if (option.spellData.getSpell().equals(fireShield)) {
                return fireShield.getLevelFor(option.spellData.getLevel(), player);
            }
        }
        return 0;
    }
}
