package com.lin.heroic_spirit_spell;

import com.lin.heroic_spirit_spell.client.DualShieldItemProperties;
import com.lin.heroic_spirit_spell.client.GravityCageClientState;
import com.lin.heroic_spirit_spell.client.GravityCageTrapHud;
import com.lin.heroic_spirit_spell.client.IncorporeityInputLock;
import com.lin.heroic_spirit_spell.client.particle.GravitationStrikeParticle;
import com.lin.heroic_spirit_spell.client.particle.HolyRushSlashParticle;
import com.lin.heroic_spirit_spell.entity.spells.EnderMoonlightChargeCutRenderer;
import com.lin.heroic_spirit_spell.entity.spells.HolySlashRenderer;
import com.lin.heroic_spirit_spell.registry.ModEntities;
import com.lin.heroic_spirit_spell.registry.ModParticles;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod(value = HeroicSpiritSpell.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = HeroicSpiritSpell.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class HeroicSpiritSpellClient {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        HeroicSpiritSpell.LOGGER.info("Heroic Spirit Spell Client Initialized");

        DualShieldItemProperties.registerBlockingPredicateForAllShields();

        EntityRenderers.register(ModEntities.HOLY_SLASH_PROJECTILE.get(), HolySlashRenderer::new);
        EntityRenderers.register(ModEntities.ENDER_MOONLIGHT_CHARGE_CUT.get(), EnderMoonlightChargeCutRenderer::new);

        // 显式注册：避免仅依赖 @EventBusSubscriber 扫描时与双 @Mod 入口竞态导致囚笼提示不绘制
        NeoForge.EVENT_BUS.addListener(GravityCageTrapHud::onRenderGuiPost);
        NeoForge.EVENT_BUS.addListener(GravityCageClientDisconnect::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(IncorporeityInputLock::onMovementInputUpdate);
    }

    /** 神圣冲锋主斩击：独立粒子类，余烬为 cleanse；纹理由 holy_rush_slash.json 指定 */
    @SubscribeEvent
    public static void registerParticleProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModParticles.HOLY_RUSH_SLASH.get(), HolyRushSlashParticle.Provider::new);
        event.registerSpriteSet(ModParticles.GRAVITATION_STRIKE_SLASH.get(), GravitationStrikeParticle.Provider::new);
    }
}

/** Reset cage HUD flag when disconnecting (server may not send false in time). */
final class GravityCageClientDisconnect {
    private GravityCageClientDisconnect() {
    }

    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        GravityCageClientState.setTrappedInEnemyCage(false);
    }
}
