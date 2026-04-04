package com.lin.heroic_spirit_spell;

import com.lin.heroic_spirit_spell.client.particle.HolyRushSlashParticle;
import com.lin.heroic_spirit_spell.entity.spells.HolySlashRenderer;
import com.lin.heroic_spirit_spell.registry.ModEntities;
import com.lin.heroic_spirit_spell.registry.ModParticles;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@Mod(value = HeroicSpiritSpell.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = HeroicSpiritSpell.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class HeroicSpiritSpellClient {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        HeroicSpiritSpell.LOGGER.info("Heroic Spirit Spell Client Initialized");

        EntityRenderers.register(ModEntities.HOLY_SLASH_PROJECTILE.get(), HolySlashRenderer::new);
    }

    /** 神圣冲锋主斩击：独立粒子类，余烬为 cleanse；纹理由 holy_rush_slash.json 指定 */
    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.HOLY_RUSH_SLASH.get(), HolyRushSlashParticle.Provider::new);
    }
}
