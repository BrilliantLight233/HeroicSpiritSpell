package com.lin.heroic_spirit_spell.spells.ender;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.particle.GravitationStrikeParticleOptions;
import com.lin.heroic_spirit_spell.spells.holy.HoldCastSpell;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.particle.TraceParticleOptions;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class GravitationStrikeSpell extends HoldCastSpell {
    private static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(HeroicSpiritSpell.MODID, "gravitation_strike");
    private static final int MAX_HOLD_DURATION_TICKS = 20 * 60 * 60;
    private static final int MAX_CHARGE_TICKS = 80;
    private static final int CHARGE_EFFECT_DURATION_TICKS = 5;
    private static final float PULL_RADIUS = 8.0f;
    private static final float DASH_DISTANCE = 12.0f;
    private static final float DASH_DAMAGE_RADIUS = 2.5f;
    private static final float LIVING_PULL_STRENGTH = 0.18f;
    private static final float PROJECTILE_PULL_STRENGTH = 0.24f;
    private static final float DASH_VERTICAL_LIFT = 0.1f;
    private static final float DAMAGE_BOX_HEIGHT_OFFSET = 1.0f;
    private static final double TARGET_INTERPOLATION_FACTOR = 0.5;
    private static final float KNOCKBACK_MULTIPLIER = 1.2f;
    private static final double MIN_PULL_DISTANCE = 0.25;
    private static final Vector3f TRACE_COLOR = new Vector3f(0.72f, 0.28f, 1.0f);
    private static final Map<UUID, ChargeState> CHARGE_STATES = new HashMap<>();

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.ENDER_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(4)
            .build();

    public GravitationStrikeSpell() {
        super(MAX_CHARGE_TICKS);
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 5;
        this.baseManaCost = 0;
        this.manaCostPerLevel = 0;
    }

    @Override
    public DefaultConfig getDefaultConfig() {
        return defaultConfig;
    }

    @Override
    public ResourceLocation getSpellResource() {
        return SPELL_ID;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("ui.irons_spellbooks.damage", Utils.stringTruncation(getBaseDamage(spellLevel, caster), 1)),
                Component.translatable("spell.heroic_spirit_spell.gravitation_strike.charge_time", Utils.stringTruncation(MAX_CHARGE_TICKS / 20f, 1)));
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.BLACK_HOLE_CHARGE.get());
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundRegistry.ENDER_CAST.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ONE_HANDED_VERTICAL_UPSWING_ANIMATION;
    }

    @Override
    public void onClientCast(Level level, int spellLevel, LivingEntity entity, ICastData castData) {
        super.onClientCast(level, spellLevel, entity, castData);
        entity.setYBodyRot(entity.getYRot());
    }

    @Override
    public void onServerPreCast(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        super.onServerPreCast(level, spellLevel, entity, playerMagicData);
        if (entity instanceof ServerPlayer serverPlayer) {
            CHARGE_STATES.put(serverPlayer.getUUID(), new ChargeState(serverPlayer.getHealth()));
        }
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (!(entity instanceof ServerPlayer serverPlayer) || playerMagicData == null) {
            return;
        }

        ChargeState state = CHARGE_STATES.computeIfAbsent(serverPlayer.getUUID(), key -> new ChargeState(serverPlayer.getHealth()));
        updateTrackedDamage(serverPlayer, state);
        applyChargeEffects(serverPlayer);
        pullNearbyTargets(level, serverPlayer);
        spawnChargeParticles(level, serverPlayer, playerMagicData);
        super.onServerCastTick(level, spellLevel, entity, playerMagicData);
    }

    @Override
    protected boolean shouldRelease(ServerPlayer serverPlayer, MagicData playerMagicData) {
        return getChargedTicks(playerMagicData) >= MAX_CHARGE_TICKS;
    }

    @Override
    public void onServerCastComplete(Level level, int spellLevel, LivingEntity entity, MagicData playerMagicData, boolean cancelled) {
        if (entity instanceof ServerPlayer serverPlayer) {
            ChargeState state = CHARGE_STATES.remove(serverPlayer.getUUID());
            if (state != null) {
                updateTrackedDamage(serverPlayer, state);
            }
            if (cancelled) {
                releaseStrike(level, spellLevel, serverPlayer, state);
            }
        }
        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }

    private void releaseStrike(Level level, int spellLevel, ServerPlayer caster, @Nullable ChargeState state) {
        float absorbedDamage = state != null ? state.accumulatedDamage : 0.0f;
        Vec3 forward = caster.getForward();
        Vec3 eyePosition = caster.getEyePosition();
        Vec3 end = Utils.raycastForBlock(
                level,
                eyePosition,
                eyePosition.add(forward.scale(DASH_DISTANCE)),
                ClipContext.Fluid.NONE).getLocation();

        AABB targetSearchArea = caster.getHitbox().expandTowards(end.subtract(eyePosition)).inflate(2.0);
        var targetableEntities = level.getEntities(caster, targetSearchArea, target ->
                target instanceof LivingEntity livingEntity
                        && isEnemyTarget(caster, livingEntity)
                        && target.getBoundingBox().getCenter()
                        .subtract(caster.getBoundingBox().getCenter())
                        .normalize()
                        .dot(caster.getForward()) >= .75);
        targetableEntities.sort(Comparator.comparingDouble(target -> target.distanceToSqr(caster)));

        boolean struckTarget = false;
        if (!targetableEntities.isEmpty() && targetableEntities.get(0).distanceToSqr(caster) < DASH_DISTANCE * DASH_DISTANCE) {
            Entity closestEntity = targetableEntities.get(0);
            AABB damageBox = AABB.ofSize(
                    closestEntity.getBoundingBox().getCenter(),
                    DASH_DAMAGE_RADIUS,
                    DASH_DAMAGE_RADIUS + DAMAGE_BOX_HEIGHT_OFFSET,
                    DASH_DAMAGE_RADIUS).move(forward.scale(DASH_DAMAGE_RADIUS / 2.0f));
            end = damageBox.getCenter().add(end).scale(TARGET_INTERPOLATION_FACTOR);
            var damageSource = getDamageSource(caster);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, damageBox, livingEntity ->
                    isEnemyTarget(caster, livingEntity)
                            && Utils.hasLineOfSight(level, caster.getEyePosition(), livingEntity.getBoundingBox().getCenter(), true))) {
                if (DamageSources.applyDamage(target, getBaseDamage(spellLevel, caster) + absorbedDamage, damageSource)) {
                    struckTarget = true;
                    MagicManager.spawnParticles(level, ParticleHelper.ENDER_SPARKS,
                            target.getX(),
                            target.getY() + target.getBbHeight() * 0.5f,
                            target.getZ(),
                            15,
                            target.getBbWidth() * 0.5f,
                            target.getBbHeight() * 0.5f,
                            target.getBbWidth() * 0.5f,
                            0.25,
                            false);
                    if (level instanceof ServerLevel serverLevel) {
                        EnchantmentHelper.doPostAttackEffects(serverLevel, target, damageSource);
                    }
                    Vec3 knockback = forward.scale(KNOCKBACK_MULTIPLIER
                                    * Utils.clampedKnockbackResistanceFactor(target, 0.2f, 1f))
                            .add(0, 0.2, 0);
                    target.setDeltaMovement(knockback);
                    target.hurtMarked = true;
                }
            }
            if (struckTarget) {
                level.playSound(null, closestEntity.getX(), closestEntity.getY(), closestEntity.getZ(),
                        SoundRegistry.FIRE_DAGGER_PARRY.get(), caster.getSoundSource());
            }
        }

        Vec3 rayVector = end.subtract(eyePosition);
        Vec3 impulse = rayVector.scale(1 / 6f).add(0, DASH_VERTICAL_LIFT, 0);
        caster.setDeltaMovement(caster.getDeltaMovement().scale(0.2).add(impulse));
        caster.hurtMarked = true;
        caster.addEffect(new MobEffectInstance(MobEffectRegistry.FALL_DAMAGE_IMMUNITY, 20, 0, false, false, true));
        spawnReleaseParticles(level, caster, end, impulse);
    }

    private void applyChargeEffects(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, CHARGE_EFFECT_DURATION_TICKS, 3, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CHARGE_EFFECT_DURATION_TICKS, 3, false, false, true));
    }

    private void pullNearbyTargets(Level level, ServerPlayer caster) {
        AABB area = caster.getBoundingBox().inflate(PULL_RADIUS);
        Vec3 casterCenter = caster.getBoundingBox().getCenter();
        for (Entity target : level.getEntities(caster, area, entity -> isPullTarget(caster, entity))) {
            Vec3 targetCenter = target.getBoundingBox().getCenter();
            Vec3 offset = casterCenter.subtract(targetCenter);
            double distance = Math.max(MIN_PULL_DISTANCE, offset.length());
            Vec3 pull = offset.normalize().scale((1.0 - Math.min(distance, PULL_RADIUS) / PULL_RADIUS)
                    * (target instanceof Projectile ? PROJECTILE_PULL_STRENGTH : LIVING_PULL_STRENGTH));
            target.setDeltaMovement(target.getDeltaMovement().scale(0.85).add(pull));
            target.hurtMarked = true;
        }
    }

    private void spawnChargeParticles(Level level, ServerPlayer caster, MagicData magicData) {
        float progress = getChargedTicks(magicData) / (float) MAX_CHARGE_TICKS;
        Vec3 center = caster.getBoundingBox().getCenter().add(0, 0.35, 0);
        float radius = 1.2f + progress * 0.8f;
        for (int i = 0; i < 6; i++) {
            double angle = caster.tickCount * 0.2 + (Math.PI * 2.0 * i / 6.0);
            double x = center.x + Math.cos(angle) * radius;
            double y = center.y + Math.sin(angle * 1.5) * 0.2 + progress * 0.25;
            double z = center.z + Math.sin(angle) * radius;
            MagicManager.spawnParticles(level, ParticleHelper.ENDER_SPARKS, x, y, z, 1, 0, 0, 0, 0.02, false);
        }
    }

    private void spawnReleaseParticles(Level level, LivingEntity caster, Vec3 end, Vec3 impulse) {
        Vec3 forward = impulse.normalize();
        Vec3 up = new Vec3(0, 1, 0);
        if (forward.dot(up) > .999) {
            up = new Vec3(1, 0, 0);
        }
        Vec3 right = up.cross(forward);
        Vec3 particlePos = end.subtract(forward.scale(3)).add(right.scale(-0.3));
        MagicManager.spawnParticles(level,
                new GravitationStrikeParticleOptions(
                        (float) forward.x,
                        (float) forward.y,
                        (float) forward.z,
                        (float) right.x,
                        (float) right.y,
                        (float) right.z,
                        1f),
                particlePos.x, particlePos.y + 0.3, particlePos.z, 1, 0, 0, 0, 0, true);

        Vec3 rayVector = end.subtract(caster.getEyePosition());
        double speed = rayVector.length() / DASH_DISTANCE * 0.75;
        for (int i = 0; i < 15; i++) {
            Vec3 particleStart = caster.getBoundingBox().getCenter().add(Utils.getRandomVec3(1 + caster.getBbWidth()));
            Vec3 particleEnd = particleStart.add(rayVector);
            MagicManager.spawnParticles(level,
                    new TraceParticleOptions(Utils.v3f(particleEnd), TRACE_COLOR),
                    particleStart.x, particleStart.y, particleStart.z, 1, 0, 0, 0, speed, false);
        }
    }

    private void updateTrackedDamage(ServerPlayer player, ChargeState state) {
        float health = player.getHealth();
        if (health < state.lastHealth) {
            state.accumulatedDamage += state.lastHealth - health;
        }
        state.lastHealth = health;
    }

    private boolean isEnemyTarget(LivingEntity caster, LivingEntity target) {
        return target.isAlive() && target != caster && !target.isAlliedTo(caster);
    }

    private boolean isPullTarget(LivingEntity caster, Entity target) {
        if (target == caster || !target.isAlive() || target.isSpectator()) {
            return false;
        }
        if (target instanceof LivingEntity livingEntity) {
            return !livingEntity.isAlliedTo(caster);
        }
        if (target instanceof Projectile projectile) {
            Entity owner = projectile.getOwner();
            return owner == null || !owner.isAlliedTo(caster);
        }
        return false;
    }

    private float getBaseDamage(int spellLevel, LivingEntity caster) {
        return getSpellPower(spellLevel, caster);
    }

    private int getChargedTicks(MagicData playerMagicData) {
        return Math.min(MAX_CHARGE_TICKS,
                Math.max(0, playerMagicData.getCastDuration() - playerMagicData.getCastDurationRemaining()));
    }

    private static final class ChargeState {
        private float accumulatedDamage;
        private float lastHealth;

        private ChargeState(float lastHealth) {
            this.lastHealth = lastHealth;
        }
    }
}
