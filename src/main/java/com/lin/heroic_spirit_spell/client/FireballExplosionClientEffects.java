package com.lin.heroic_spirit_spell.client;

import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import io.redspace.ironsspellbooks.util.MinecraftInstanceHelper;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * Client-side fireball explosion VFX matching {@link io.redspace.ironsspellbooks.player.ClientSpellCastHelper#handleClientboundFieryExplosion},
 * with {@link ParticleHelper#FIERY_SMOKE} iteration counts scaled to 25% of vanilla.
 */
public final class FireballExplosionClientEffects {

    private static final double FIERY_SMOKE_FRACTION = 0.25;

    private FireballExplosionClientEffects() {
    }

    private static int scaleSmokeIterations(int n) {
        return Math.max(1, (int) Math.ceil(n * FIERY_SMOKE_FRACTION));
    }

    public static void play(Vec3 pos, float radius) {
        MinecraftInstanceHelper.ifPlayerPresent(player -> {
            var level = player.level();
            double x = pos.x;
            double y = pos.y;
            double z = pos.z;
            level.addParticle(new BlastwaveParticleOptions(new Vector3f(1, .6f, 0.3f), radius + 1), x, y, z, 0, 0, 0);

            int c = (int) (6.28 * radius) * 2;
            int cSmoke = scaleSmokeIterations(c);
            float step = 360f / cSmoke * Mth.DEG_TO_RAD;
            float speed = (0.06f + 0.01f * radius) * 2;
            for (int i = 0; i < cSmoke; i++) {
                Vec3 vec3 = new Vec3(Mth.cos(step * i), 0, Mth.sin(step * i)).scale(speed);
                Vec3 posOffset = Utils.getRandomVec3(.5f).add(vec3.scale(10));
                vec3 = vec3.add(Utils.getRandomVec3(0.01));
                level.addParticle(ParticleHelper.FIERY_SMOKE, x + posOffset.x, y + posOffset.y, z + posOffset.z, vec3.x, vec3.y, vec3.z);
            }

            int cloudDensity = 50 + (int) (25 * radius);
            int smokeCloudDensity = scaleSmokeIterations(cloudDensity);
            for (int i = 0; i < smokeCloudDensity; i++) {
                Vec3 posOffset = Utils.getRandomVec3(1).scale(radius * .125f);
                Vec3 motion = posOffset.normalize().scale(speed * .5f);
                posOffset = posOffset.add(motion.scale(Utils.getRandomScaled(1)));
                motion = motion.add(Utils.getRandomVec3(speed * .1f));
                level.addParticle(ParticleHelper.FIERY_SMOKE, x + posOffset.x, y + posOffset.y, z + posOffset.z, motion.x, motion.y, motion.z);
            }

            for (int i = 0; i < cloudDensity; i += 2) {
                Vec3 posOffset = Utils.getRandomVec3(1).scale(radius * .4f);
                Vec3 motion = posOffset.normalize().scale(speed * .5f);
                motion = motion.add(Utils.getRandomVec3(0.25));
                level.addParticle(ParticleHelper.EMBERS, true, x + posOffset.x, y + posOffset.y, z + posOffset.z, motion.x, motion.y, motion.z);
                level.addParticle(ParticleHelper.FIRE, x + posOffset.x * .5f, y + posOffset.y * .5f, z + posOffset.z * .5f, motion.x, motion.y, motion.z);
            }

            for (int i = 0; i < cloudDensity; i += 2) {
                Vec3 posOffset = Utils.getRandomVec3(radius).scale(.2f);
                Vec3 motion = posOffset.normalize().scale(0.8);
                motion = motion.add(Utils.getRandomVec3(0.18));
                level.addParticle(ParticleHelper.FIERY_SPARKS, x + posOffset.x * .5f, y + posOffset.y * .5f, z + posOffset.z * .5f, motion.x, motion.y, motion.z);
            }
        });
    }
}
