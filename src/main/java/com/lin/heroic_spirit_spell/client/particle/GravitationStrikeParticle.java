package com.lin.heroic_spirit_spell.client.particle;

import com.lin.heroic_spirit_spell.particle.GravitationStrikeParticleOptions;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

@OnlyIn(Dist.CLIENT)
public class GravitationStrikeParticle extends TextureSheetParticle {
    private static final float TRAIL_CURVE_START_SCALE = 0.4f;
    private static final float TRAIL_CURVE_END_SCALE = 0.85f;

    private final SpriteSet sprites;
    private final Vec3 forward;
    private final Vec3 up;
    private final Vector3f[] localVertices;

    GravitationStrikeParticle(ClientLevel level, double x, double y, double z, SpriteSet spriteSet,
                              double xd, double yd, double zd, GravitationStrikeParticleOptions options) {
        super(level, x, y, z, 0, 0, 0);
        this.lifetime = 5;
        this.gravity = 0;
        this.sprites = spriteSet;
        this.quadSize = options.scale * 3.25f;
        this.forward = new Vec3(options.xf, options.yf, options.zf).normalize();
        this.up = new Vec3(options.xu, options.yu, options.zu).normalize();
        this.localVertices = calculateVertices();
        if (new Vec3(xd, yd, zd).lengthSqr() > 0) {
            this.xd = xd;
            this.yd = yd;
            this.zd = zd;
        } else {
            this.xd = this.forward.x * .1;
            this.yd = this.forward.y * .1;
            this.zd = this.forward.z * .1;
        }
        this.friction = 1;
    }

    @Override
    public void tick() {
        if (this.age == 0) {
            createEnderEmberTrail();
        }
        this.move(this.xd, this.yd, this.zd);
        if (this.age++ > this.lifetime) {
            this.remove();
        } else {
            this.setSpriteFromAge(this.sprites);
        }
    }

    private void createEnderEmberTrail() {
        int particleCount = (int) (15 * this.quadSize);
        for (int i = 1; i < particleCount - 1; i++) {
            float t = i / (float) particleCount;
            Vec3 localPos = calculateBezierPoint(t).scale(this.quadSize * TRAIL_CURVE_END_SCALE);
            Vec3 pos = localPos.add(Utils.getRandomVec3(0.2 + i * .01f));
            Vec3 motion = new Vec3(this.xd, this.yd, this.zd).scale(random.nextDouble() * 6);
            if (random.nextFloat() < .6f) {
                level.addParticle(ParticleHelper.ENDER_SPARKS,
                        x + pos.x, y + pos.y, z + pos.z,
                        motion.x * 1.25, motion.y * 1.25, motion.z * 1.25);
            }
        }
    }

    private Vec3 calculateBezierPoint(float t) {
        float u = 1 - t;
        return vec3Copy(localVertices[1]).scale(TRAIL_CURVE_START_SCALE).scale(u * u * u).add(
                vec3Copy(localVertices[2]).scale(3 * u * u * t).add(
                        vec3Copy(localVertices[3]).scale(3 * u * t * t).add(
                                vec3Copy(localVertices[0]).scale(TRAIL_CURVE_END_SCALE).scale(t * t * t)
                        )
                )
        );
    }

    private Vector3f[] calculateVertices() {
        Vec3 right = this.forward.cross(this.up);
        Vector3f[] vertices = new Vector3f[]{
                new Vector3f(-1.0F, -1.0F, 0.0F), new Vector3f(-1.0F, 1.0F, 0.0F),
                new Vector3f(1.0F, 1.0F, 0.0F), new Vector3f(1.0F, -1.0F, 0.0F)
        };
        for (int i = 0; i < 4; i++) {
            float x = (float) (forward.x * vertices[i].x + right.x * vertices[i].y);
            float y = (float) (forward.y * vertices[i].x + right.y * vertices[i].y);
            float z = (float) (forward.z * vertices[i].x + right.z * vertices[i].y);
            vertices[i] = new Vector3f(x, y, z);
        }
        return vertices;
    }

    private Vec3 vec3Copy(Vector3f vector3f) {
        return new Vec3(vector3f.x, vector3f.y, vector3f.z);
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTick) {
        Vec3 cameraPos = camera.getPosition();
        float x = (float) (Mth.lerp(partialTick, this.xo, this.x) - cameraPos.x());
        float y = (float) (Mth.lerp(partialTick, this.yo, this.y) - cameraPos.y());
        float z = (float) (Mth.lerp(partialTick, this.zo, this.z) - cameraPos.z());
        Vector3f[] vertices = new Vector3f[4];
        for (int i = 0; i < 4; i++) {
            Vector3f localVertex = localVertices[i];
            vertices[i] = new Vector3f(localVertex.x, localVertex.y, localVertex.z);
            vertices[i].mul(this.getQuadSize(partialTick));
            vertices[i].add(x, y, z);
        }
        int light = this.getLightColor(partialTick);
        makeCornerVertex(buffer, vertices[0], this.getU1(), this.getV1(), light);
        makeCornerVertex(buffer, vertices[1], this.getU1(), this.getV0(), light);
        makeCornerVertex(buffer, vertices[2], this.getU0(), this.getV0(), light);
        makeCornerVertex(buffer, vertices[3], this.getU0(), this.getV1(), light);
        makeCornerVertex(buffer, vertices[3], this.getU0(), this.getV1(), light);
        makeCornerVertex(buffer, vertices[2], this.getU0(), this.getV0(), light);
        makeCornerVertex(buffer, vertices[1], this.getU1(), this.getV0(), light);
        makeCornerVertex(buffer, vertices[0], this.getU1(), this.getV1(), light);
    }

    private void makeCornerVertex(VertexConsumer consumer, Vector3f vec, float u, float v, int light) {
        consumer.addVertex(vec.x(), vec.y(), vec.z()).setUv(u, v).setColor(this.rCol, this.gCol, this.bCol, this.alpha).setLight(light);
    }

    @Override
    public @NotNull ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class Provider implements ParticleProvider<GravitationStrikeParticleOptions> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(GravitationStrikeParticleOptions options,
                                       @NotNull ClientLevel level, double x, double y, double z, double xd, double yd, double zd) {
            GravitationStrikeParticle particle = new GravitationStrikeParticle(level, x, y, z, this.sprites, xd, yd, zd, options);
            particle.setSpriteFromAge(this.sprites);
            particle.setAlpha(1.0F);
            return particle;
        }
    }
}
