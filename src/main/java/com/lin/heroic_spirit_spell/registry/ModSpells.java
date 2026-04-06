package com.lin.heroic_spirit_spell.registry;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.spells.holy.HolyShieldSpell;
import com.lin.heroic_spirit_spell.spells.holy.HolyRushSpell;
import com.lin.heroic_spirit_spell.spells.holy.HolySlashSpell;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * 法术注册表
 */
public class ModSpells {
    // 创建法术注册表的 ResourceKey
    private static final ResourceKey<Registry<AbstractSpell>> SPELL_REGISTRY_KEY = 
        ResourceKey.createRegistryKey(ResourceLocation.parse("irons_spellbooks:spells"));
    
    public static final DeferredRegister<AbstractSpell> SPELLS = 
        DeferredRegister.create(
            SPELL_REGISTRY_KEY, 
            HeroicSpiritSpell.MODID
        );
    
    public static final DeferredHolder<AbstractSpell, HolySlashSpell> HOLY_SLASH =
            SPELLS.register("holy_slash", HolySlashSpell::new);

    public static final DeferredHolder<AbstractSpell, HolyRushSpell> HOLY_RUSH =
            SPELLS.register("holy_rush", HolyRushSpell::new);

    public static final DeferredHolder<AbstractSpell, HolyShieldSpell> HOLY_SHIELD =
            SPELLS.register("holy_shield", HolyShieldSpell::new);
    
    // TODO: 在这里添加新的法术注册
    // 示例:
    // public static final DeferredHolder<AbstractSpell, YourSpell> YOUR_SPELL = SPELLS.register("your_spell", 
    //     YourSpell::new);
    
    public static void register(IEventBus modEventBus) {
        SPELLS.register(modEventBus);
        HeroicSpiritSpell.LOGGER.info("ModSpells initialized with {} spells", SPELLS.getEntries().size());
        
    }
}
