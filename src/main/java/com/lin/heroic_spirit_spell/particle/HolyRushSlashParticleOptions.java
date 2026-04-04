package com.lin.heroic_spirit_spell.particle;

import com.lin.heroic_spirit_spell.registry.ModParticles;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.codec.StreamCodec;
import org.jetbrains.annotations.NotNull;

/**
 * 与铁魔法 {@code EnderSlashParticleOptions} 字段一致，绑定本模组粒子类型（纹理 holy_rush_1～5）。
 * 客户端渲染通过 {@link io.redspace.ironsspellbooks.particle.EnderSlashParticle.Provider} 委托实现。
 */
public class HolyRushSlashParticleOptions implements ParticleOptions {

    public final float scale;
    public final float xf;
    public final float yf;
    public final float zf;
    public final float xu;
    public final float yu;
    public final float zu;

    public HolyRushSlashParticleOptions(float xf, float yf, float zf, float xu, float yu, float zu, float scale) {
        this.scale = scale;
        this.xf = xf;
        this.yf = yf;
        this.zf = zf;
        this.xu = xu;
        this.yu = yu;
        this.zu = zu;
    }

    public static final StreamCodec<ByteBuf, HolyRushSlashParticleOptions> STREAM_CODEC = StreamCodec.of(
            (buf, option) -> {
                buf.writeFloat(option.xf);
                buf.writeFloat(option.yf);
                buf.writeFloat(option.zf);
                buf.writeFloat(option.xu);
                buf.writeFloat(option.yu);
                buf.writeFloat(option.zu);
                buf.writeFloat(option.scale);
            },
            buf -> new HolyRushSlashParticleOptions(
                    buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat(), buf.readFloat(), buf.readFloat(),
                    buf.readFloat())
    );

    public static final MapCodec<HolyRushSlashParticleOptions> MAP_CODEC = RecordCodecBuilder.mapCodec(object ->
            object.group(
                    Codec.FLOAT.fieldOf("xf").forGetter(p -> p.xf),
                    Codec.FLOAT.fieldOf("yf").forGetter(p -> p.yf),
                    Codec.FLOAT.fieldOf("zf").forGetter(p -> p.zf),
                    Codec.FLOAT.fieldOf("xu").forGetter(p -> p.xu),
                    Codec.FLOAT.fieldOf("yu").forGetter(p -> p.yu),
                    Codec.FLOAT.fieldOf("zu").forGetter(p -> p.zu),
                    Codec.FLOAT.fieldOf("scale").forGetter(p -> p.scale)
            ).apply(object, HolyRushSlashParticleOptions::new)
    );

    @Override
    public @NotNull ParticleType<?> getType() {
        return ModParticles.HOLY_RUSH_SLASH.get();
    }
}
