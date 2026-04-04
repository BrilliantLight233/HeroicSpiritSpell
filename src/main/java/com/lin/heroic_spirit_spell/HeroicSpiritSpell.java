package com.lin.heroic_spirit_spell;

import org.slf4j.Logger;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.ModContainer;
import com.lin.heroic_spirit_spell.registry.ModSpells;
import com.lin.heroic_spirit_spell.registry.ModEntities;
import com.lin.heroic_spirit_spell.registry.ModParticles;

@Mod(HeroicSpiritSpell.MODID)
public class HeroicSpiritSpell {
    public static final String MODID = "heroic_spirit_spell";
    public static final Logger LOGGER = LogUtils.getLogger();

    public HeroicSpiritSpell(IEventBus modEventBus, ModContainer modContainer) {
        // 注册通用设置
        modEventBus.addListener(this::commonSetup);
        
        // 注册法术系统
        ModSpells.register(modEventBus);

        // 注册实体
        ModEntities.register(modEventBus);

        ModParticles.register(modEventBus);

        // 注册到 NeoForge 事件总线
        // NeoForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("Heroic Spirit Spell Mod Initialized");
    }
}
