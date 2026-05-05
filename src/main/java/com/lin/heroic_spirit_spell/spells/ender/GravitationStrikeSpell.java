package com.lin.heroic_spirit_spell.spells.ender;

import com.github.L_Ender.cataclysm.entity.effect.Void_Vortex_Entity;
import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.registry.ModEffects;
import com.lin.heroic_spirit_spell.spells.holy.HoldCastSpell;
import com.lin.heroic_spirit_spell.util.GravitationStrikeChargeTracker;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.ImpulseCastData;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.network.casting.OnClientCastPacket;
import io.redspace.ironsspellbooks.player.SpinAttackType;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class GravitationStrikeSpell extends HoldCastSpell {
    private static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(HeroicSpiritSpell.MODID, "gravitation_strike");
    private static final int MAX_HOLD_DURATION_TICKS = 20 * 60 * 60;
    private static final int MAX_CHARGE_TICKS = 80;
    private static final int CHARGE_EFFECT_DURATION_TICKS = 5;
    /** 与铁魔法烈焰冲锋一致：冲刺状态持续 15 tick */
    private static final int DASH_EFFECT_DURATION_TICKS = 15;
    private static final float PULL_RADIUS = 8.0f;
    private static final float DASH_BASE_POWER = 15.0f;
    private static final float LIVING_PULL_STRENGTH = 0.18f;
    private static final float PROJECTILE_PULL_STRENGTH = 0.24f;
    private static final double DASH_VECTOR_HORIZONTAL_SCALE = 3.0d;
    private static final double DASH_VECTOR_VERTICAL_BIAS = 0.25d;
    private static final double DASH_DELTA_LERP_FACTOR = 0.75d;
    private static final double GROUND_LAUNCH_HEIGHT = 1.5d;
    private static final double MIN_PULL_DISTANCE = 0.25;
    private static final ResourceLocation ENDER_RIPTIDE_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(HeroicSpiritSpell.MODID, "textures/entity/ender_riptide.png");
    private static final SpinAttackType ENDER_SPIN_ATTACK = new SpinAttackType(ENDER_RIPTIDE_TEXTURE, true);

    /** Cataclysm void_vortex: refresh lifespan to avoid natural expiry explosion */
    private static final int VOID_VORTEX_REFRESH_TICKS = 600;
    /** Caster UUID -> vortex entity id (server thread only) */
    private static final ConcurrentHashMap<UUID, Integer> VORTEX_ENTITY_ID_BY_CASTER = new ConcurrentHashMap<>();

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

    /**
     * 与 {@link #releaseStrike} 一致的水平冲锋方向（单位向量，xz）。
     */
    public static Vec3 getDashHorizontalDirection(LivingEntity caster) {
        Vec3 look = caster.getLookAngle();
        Vec3 scaled = look.multiply(DASH_VECTOR_HORIZONTAL_SCALE, 1.0, DASH_VECTOR_HORIZONTAL_SCALE).normalize();
        Vec3 horiz = new Vec3(scaled.x, 0.0, scaled.z);
        if (horiz.lengthSqr() < 1e-8) {
            float yawRad = caster.getYRot() * Mth.DEG_TO_RAD;
            return new Vec3(-Mth.sin(yawRad), 0.0, Mth.cos(yawRad));
        }
        return horiz.normalize();
    }

    /**
     * 粗略估算本段冲刺的水平位移（格）。dashDamageAmplifier 为冲刺效果的伤害值（与 getDashContactDamage 一致），用于反推法术强度。
     * 与当前速度取较大值，避免估算偏低。
     */
    public static double estimateHorizontalDashTravelBlocks(LivingEntity caster, int dashDamageAmplifier) {
        float spellPowerApprox = Math.max(0.0f, dashDamageAmplifier - 5.0f);
        float dashPower = (DASH_BASE_POWER + spellPowerApprox) / 12.0f;
        Vec3 look = caster.getLookAngle();
        Vec3 scaled = look.multiply(DASH_VECTOR_HORIZONTAL_SCALE, 1.0, DASH_VECTOR_HORIZONTAL_SCALE).normalize();
        Vec3 impulse = scaled.add(0.0, DASH_VECTOR_VERTICAL_BIAS, 0.0).scale(dashPower);
        double hSpeedFromImpulse = Math.hypot(impulse.x, impulse.z);
        double fromImpulse = hSpeedFromImpulse * DASH_EFFECT_DURATION_TICKS * 0.4;
        double vx = caster.getDeltaMovement().x;
        double vz = caster.getDeltaMovement().z;
        double velBased = Math.hypot(vx, vz) * DASH_EFFECT_DURATION_TICKS * 0.42;
        return Math.max(fromImpulse, velBased);
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
                Component.translatable("ui.irons_spellbooks.damage", getDashContactDamage(spellLevel, caster)),
                Component.translatable("spell.heroic_spirit_spell.gravitation_strike.charge_time", Utils.stringTruncation(MAX_CHARGE_TICKS / 20f, 1)));
    }

    // 对齐 Cataclysm_Spellbooks GravitationPull：finish=PORTAL_AMBIENT，无 getCastStartSound
    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.of(SoundEvents.PORTAL_AMBIENT);
    }

    // 与 dragon_breath 相同：AbstractSpell 对持续施法的默认动作（continuous_thrust）
    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_CONTINUOUS_CAST;
    }

    @Override
    public void onClientCast(Level level, int spellLevel, LivingEntity entity, ICastData castData) {
        if (castData instanceof ImpulseCastData impulseCastData) {
            entity.hasImpulse = impulseCastData.hasImpulse;
            entity.setDeltaMovement(entity.getDeltaMovement().add(impulseCastData.x, impulseCastData.y, impulseCastData.z));
            entity.setYBodyRot(entity.getYRot());
            return;
        }
        super.onClientCast(level, spellLevel, entity, castData);
        entity.setYBodyRot(entity.getYRot());
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new ImpulseCastData();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }

    @Override
    public void onServerPreCast(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        super.onServerPreCast(level, spellLevel, entity, playerMagicData);
        if (entity instanceof ServerPlayer serverPlayer) {
            GravitationStrikeChargeTracker.resetForNewCast(serverPlayer);
        }
    }

    @Override
    public void onServerCastTick(Level level, int spellLevel, LivingEntity entity, @Nullable MagicData playerMagicData) {
        if (!(entity instanceof ServerPlayer serverPlayer) || playerMagicData == null) {
            return;
        }

        if (level instanceof ServerLevel serverLevel) {
            tickVoidVortex(serverLevel, serverPlayer);
        }
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
        if (entity instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            removeVoidVortex(serverLevel, serverPlayer);
        }
        if (entity instanceof ServerPlayer serverPlayer) {
            // 自然结束持续施法时 MagicManager 传 cancelled=false；CancelCastPacket 满蓄/手动取消为 true。
            // 仅判断 cancelled 会导致自然结束永远不冲锋。
            releaseStrike(spellLevel, serverPlayer, playerMagicData);
        }
        super.onServerCastComplete(level, spellLevel, entity, playerMagicData, cancelled);
    }

    private void releaseStrike(int spellLevel, ServerPlayer caster, MagicData playerMagicData) {
        CastSource castSource = playerMagicData.getCastSource();

        caster.hasImpulse = true;
        float dashPower = (DASH_BASE_POWER + getSpellPower(spellLevel, caster)) / 12.0f;
        Vec3 impulse = caster.getLookAngle()
                .multiply(DASH_VECTOR_HORIZONTAL_SCALE, 1.0d, DASH_VECTOR_HORIZONTAL_SCALE)
                .normalize()
                .add(0.0d, DASH_VECTOR_VERTICAL_BIAS, 0.0d)
                .scale(dashPower);

        if (caster.onGround()) {
            caster.setPos(caster.position().add(0.0d, GROUND_LAUNCH_HEIGHT, 0.0d));
        }

        ImpulseCastData impulseData = new ImpulseCastData(
                (float) impulse.x,
                (float) impulse.y,
                (float) impulse.z,
                true
        );

        caster.setDeltaMovement(new Vec3(
                Mth.lerp(DASH_DELTA_LERP_FACTOR, caster.getDeltaMovement().x, impulse.x),
                Mth.lerp(DASH_DELTA_LERP_FACTOR, caster.getDeltaMovement().y, impulse.y),
                Mth.lerp(DASH_DELTA_LERP_FACTOR, caster.getDeltaMovement().z, impulse.z)
        ));
        caster.hurtMarked = true;
        GravitationStrikeChargeTracker.transferChargeToDashRetribution(caster);
        caster.addEffect(new MobEffectInstance(
                ModEffects.GRAVITATION_STRIKE_DASH,
                DASH_EFFECT_DURATION_TICKS,
                getDashContactDamage(spellLevel, caster),
                false,
                false,
                false
        ));
        caster.invulnerableTime = 20;
        playerMagicData.getSyncedData().setSpinAttackType(ENDER_SPIN_ATTACK);

        caster.level().playSound(
                null,
                caster.getX(),
                caster.getY(),
                caster.getZ(),
                SoundRegistry.BLACK_HOLE_CAST.get(),
                caster.getSoundSource());

        PacketDistributor.sendToPlayer(caster, new OnClientCastPacket(getSpellId(), spellLevel, castSource, impulseData));
    }

    private void applyChargeEffects(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, CHARGE_EFFECT_DURATION_TICKS, 3, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, CHARGE_EFFECT_DURATION_TICKS, 2, false, false, true));
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

    private int getDashContactDamage(int spellLevel, LivingEntity caster) {
        return (int) (5.0f + getSpellPower(spellLevel, caster));
    }

    private int getChargedTicks(MagicData playerMagicData) {
        return Math.min(MAX_CHARGE_TICKS,
                Math.max(0, playerMagicData.getCastDuration() - playerMagicData.getCastDurationRemaining()));
    }

    private static void tickVoidVortex(ServerLevel level, ServerPlayer caster) {
        UUID casterId = caster.getUUID();
        Integer netId = VORTEX_ENTITY_ID_BY_CASTER.get(casterId);
        if (netId != null) {
            Entity existing = level.getEntity(netId);
            if (existing instanceof Void_Vortex_Entity vortex && vortex.isAlive()) {
                vortex.setPos(caster.getX(), caster.getY(), caster.getZ());
                vortex.setYRot(caster.getYRot());
                vortex.setLifespan(VOID_VORTEX_REFRESH_TICKS);
                return;
            }
            VORTEX_ENTITY_ID_BY_CASTER.remove(casterId);
        }

        float yawRad = caster.getYRot() * ((float) Math.PI / 180F);
        Void_Vortex_Entity vortex = new Void_Vortex_Entity(
                level,
                caster.getX(),
                caster.getY(),
                caster.getZ(),
                yawRad,
                caster,
                VOID_VORTEX_REFRESH_TICKS
        );
        level.addFreshEntity(vortex);
        VORTEX_ENTITY_ID_BY_CASTER.put(casterId, vortex.getId());
    }

    private static void removeVoidVortex(ServerLevel level, ServerPlayer caster) {
        Integer netId = VORTEX_ENTITY_ID_BY_CASTER.remove(caster.getUUID());
        if (netId == null) {
            return;
        }
        Entity e = level.getEntity(netId);
        if (e != null) {
            e.discard();
        }
    }

}
