package com.lin.heroic_spirit_spell.registry;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.particle.GravitationStrikeParticleOptions;
import com.lin.heroic_spirit_spell.particle.HolyRushSlashParticleOptions;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 自定义粒子类型（神圣冲锋主斩击：holy_rush_1～5 纹理序列）
 */
public final class ModParticles {

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, HeroicSpiritSpell.MODID);

    @SuppressWarnings("unchecked")
    public static final DeferredHolder<ParticleType<?>, ParticleType<HolyRushSlashParticleOptions>> HOLY_RUSH_SLASH =
            PARTICLE_TYPES.register("holy_rush_slash", () -> new ParticleType<HolyRushSlashParticleOptions>(true) {
                @Override
                public MapCodec<HolyRushSlashParticleOptions> codec() {
                    return HolyRushSlashParticleOptions.MAP_CODEC;
                }

                @Override
                public StreamCodec<RegistryFriendlyByteBuf, HolyRushSlashParticleOptions> streamCodec() {
                    return (StreamCodec<RegistryFriendlyByteBuf, HolyRushSlashParticleOptions>)
                            (StreamCodec<?, HolyRushSlashParticleOptions>) HolyRushSlashParticleOptions.STREAM_CODEC;
                }
            });

    @SuppressWarnings("unchecked")
    public static final DeferredHolder<ParticleType<?>, ParticleType<GravitationStrikeParticleOptions>> GRAVITATION_STRIKE_SLASH =
            PARTICLE_TYPES.register("gravitation_strike_slash", () -> new ParticleType<GravitationStrikeParticleOptions>(true) {
                @Override
                public MapCodec<GravitationStrikeParticleOptions> codec() {
                    return GravitationStrikeParticleOptions.MAP_CODEC;
                }

                @Override
                public StreamCodec<RegistryFriendlyByteBuf, GravitationStrikeParticleOptions> streamCodec() {
                    return (StreamCodec<RegistryFriendlyByteBuf, GravitationStrikeParticleOptions>)
                            (StreamCodec<?, GravitationStrikeParticleOptions>) GravitationStrikeParticleOptions.STREAM_CODEC;
                }
            });

    private ModParticles() {
    }

    public static void register(IEventBus modEventBus) {
        PARTICLE_TYPES.register(modEventBus);
    }
}
