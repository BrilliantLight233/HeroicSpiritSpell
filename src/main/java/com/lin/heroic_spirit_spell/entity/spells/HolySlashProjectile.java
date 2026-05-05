package com.lin.heroic_spirit_spell.entity.spells;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.lin.heroic_spirit_spell.registry.ModEntities;

import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.entity.spells.AbstractShieldEntity;
import io.redspace.ironsspellbooks.entity.spells.ShieldPart;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 神圣斩击光刃实体
 * 参考 BloodSlashProjectile，实现：
 * - 持续 5s
 * - 轨迹使用 WISP 粒子
 * - 命中时施加 irons_spellbooks:guided 效果 + 圣光粒子
 * - 贯穿路径上的所有单位
 */
public class HolySlashProjectile extends Projectile implements AntiMagicSusceptible {

    private static final EntityDataAccessor<Float> DATA_RADIUS =
            SynchedEntityData.defineId(HolySlashProjectile.class, EntityDataSerializers.FLOAT);

    private static final double SPEED = 1.6d;
    private static final int EXPIRE_TIME = 5 * 20; // 5 秒

    public final int animationSeed;
    private final float maxRadius;
    public AABB oldBB;
    private int age;
    private float damage;
    public int animationTime;
    private final List<Entity> victims;

    public HolySlashProjectile(EntityType<? extends HolySlashProjectile> type, Level level) {
        super(type, level);
        this.animationSeed = io.redspace.ironsspellbooks.api.util.Utils.random.nextInt(9999);
        this.maxRadius = 3.0f;
        this.oldBB = getBoundingBox();
        this.victims = new ArrayList<>();
        this.setNoGravity(true);
    }

    public HolySlashProjectile(Level level, LivingEntity shooter) {
        this(ModEntities.HOLY_SLASH_PROJECTILE.get(), level);
        setOwner(shooter);
        setYRot(shooter.getYRot());
        setXRot(shooter.getXRot());
    }

    public void shoot(Vec3 direction) {
        setDeltaMovement(direction.scale(SPEED));
    }

    public void setDamage(float damage) {
        this.damage = damage;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_RADIUS, 0.6F);
    }

    public void setRadius(float newRadius) {
        if (newRadius <= maxRadius && level() != null && !level().isClientSide) {
            this.getEntityData().set(DATA_RADIUS, Mth.clamp(newRadius, 0.0F, maxRadius));
        }
    }

    public float getRadius() {
        return this.getEntityData().get(DATA_RADIUS);
    }

    @Override
    public void refreshDimensions() {
        double x = this.getX();
        double y = this.getY();
        double z = this.getZ();
        super.refreshDimensions();
        this.setPos(x, y, z);
    }

    @Override
    public void tick() {
        super.tick();

        if (++age > EXPIRE_TIME) {
            discard();
            return;
        }

        oldBB = getBoundingBox();
        setRadius(getRadius() + 0.12f);

        if (!this.level().isClientSide) {
            HitResult hitResult = ProjectileUtil.getHitResultOnMoveVector(this,
                    target -> canHitEntity(target) && !victims.contains(target));
            if (hitResult.getType() == HitResult.Type.BLOCK) {
                onHitBlock((BlockHitResult) hitResult);
                return;
            }

            // 贯穿路径上的所有可命中实体
            for (Entity entity : this.level().getEntities(this, this.getBoundingBox())
                    .stream()
                    .filter(target -> canHitEntity(target) && !victims.contains(target))
                    .collect(Collectors.toSet())) {
                damageEntity(entity);

                // 命中后播净化粒子（irons_spellbooks:cleanse）
                spawnCleanseHitParticles(entity);

                if (entity instanceof ShieldPart || entity instanceof AbstractShieldEntity) {
                    discard();
                    return;
                }
            }
        }

        setPos(position().add(getDeltaMovement()));
        spawnTrailParticles();
    }

    @Override
    public EntityDimensions getDimensions(Pose pose) {
        return EntityDimensions.scalable(this.getRadius() * 2.0F, 0.5F);
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
        if (DATA_RADIUS.equals(key)) {
            this.refreshDimensions();
        }
        super.onSyncedDataUpdated(key);
    }

    private void damageEntity(Entity entity) {
        if (victims.contains(entity)) {
            return;
        }

        // 直接创建伤害源，避免 DeferredHolder.get() 可能为 null 的问题
        var source = level().damageSources().mobProjectile(this, getOwner() instanceof LivingEntity le ? le : null);

        boolean success = entity.hurt(source, damage);

        if (success) {
            victims.add(entity);

            if (entity instanceof LivingEntity living) {
                applyGuidedEffect(living);
            }
        }
    }

    /**
     * 命中后施加 irons_spellbooks:guided 效果
     */
    private void applyGuidedEffect(LivingEntity target) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        var effectRegistry = serverLevel.registryAccess()
                .registryOrThrow(Registries.MOB_EFFECT);

        effectRegistry.getHolder(ResourceLocation.parse("irons_spellbooks:guided"))
                .ifPresent(holder -> {
                    // 不使用 Holder 身份判断：跨 RegistryAccess/同步场景下可能不一致，改为按注册名判断
                    boolean alreadyGuided = target.getActiveEffects().stream()
                            .anyMatch(effect -> effect.getEffect().unwrapKey()
                                    .map(key -> key.location().equals(ResourceLocation.parse("irons_spellbooks:guided")))
                                    .orElse(false));

                    // 保持原有：命中给予/刷新 guided
                    target.addEffect(new MobEffectInstance(holder, 20 * 5, 0));

                    // 若目标已拥有 guided，则额外给予霉运 V（5s）
                    if (alreadyGuided) {
                        target.addEffect(new MobEffectInstance(MobEffects.UNLUCK, 20 * 5, 4));
                    }
                });
    }

    private void spawnCleanseHitParticles(Entity entity) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        var particleRegistry = serverLevel.registryAccess().registryOrThrow(Registries.PARTICLE_TYPE);
        particleRegistry.getOptional(ResourceLocation.parse("irons_spellbooks:cleanse"))
                .ifPresent(particleType -> {
                    if (particleType instanceof SimpleParticleType simpleParticleType) {
                        serverLevel.sendParticles(
                                simpleParticleType,
                                entity.getX(),
                                entity.getY() + entity.getBbHeight() * 0.5,
                                entity.getZ(),
                                25,
                                0.0,
                                0.0,
                                0.0,
                                0.25
                        );
                    }
                });
    }

    private void spawnCleanseAt(double x, double y, double z) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        var particleRegistry = serverLevel.registryAccess().registryOrThrow(Registries.PARTICLE_TYPE);
        particleRegistry.getOptional(ResourceLocation.parse("irons_spellbooks:cleanse"))
                .ifPresent(particleType -> {
                    if (particleType instanceof SimpleParticleType simpleParticleType) {
                        serverLevel.sendParticles(simpleParticleType, x, y, z, 25, 0.0, 0.0, 0.0, 0.25);
                    }
                });
    }

    // 轨迹粒子：WISP
    private void spawnTrailParticles() {
        if (!this.level().isClientSide || !this.isAddedToLevel()) {
            return;
        }

        AABB boundingBox = getBoundingBox();
        if (boundingBox == null) {
            return;
        }

        // 轨迹带宽减半，避免粒子过宽
        float width = (float) boundingBox.getXsize() * 1.0f;
        if (width <= 0 || Float.isNaN(width)) {
            return;
        }
        
        float step = 0.25f;
        float radians = Mth.DEG_TO_RAD * getYRot();
        float speed = 0.1f;

        for (int i = 0; i < width / step; i++) {
            double x = getX();
            double y = getY();
            double z = getZ();
            double offset = step * (i - width / step / 2);
            double rotX = offset * Math.cos(radians);
            double rotZ = -offset * Math.sin(radians);

            double dx = Math.random() * speed * 2 - speed;
            double dy = Math.random() * speed * 2 - speed;
            double dz = Math.random() * speed * 2 - speed;

            this.level().addParticle(
                    ParticleHelper.WISP,
                    false,
                    x + rotX + dx, y + dy, z + rotZ + dz,
                    dx, dy, dz
            );
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return entity != getOwner() && super.canHitEntity(entity);
    }

    @Override
    protected void onHitBlock(BlockHitResult result) {
        super.onHitBlock(result);
        spawnCleanseAt(result.getLocation().x, result.getLocation().y, result.getLocation().z);
        discard();
    }

    @Override
    public void onAntiMagic(MagicData magicData) {
        this.discard();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putFloat("Damage", this.damage);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.damage = tag.getFloat("Damage");
    }
}