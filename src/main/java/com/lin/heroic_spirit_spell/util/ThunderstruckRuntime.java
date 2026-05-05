package com.lin.heroic_spirit_spell.util;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.network.ThunderstruckCameraPayload;
import com.lin.heroic_spirit_spell.network.ThunderstruckStage2CastAnimPayload;
import com.lin.heroic_spirit_spell.registry.ModSpells;
import com.lin.heroic_spirit_spell.registry.ModEffects;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.damage.SpellDamageSource;
import io.redspace.ironsspellbooks.entity.spells.LightningStrike;
import io.redspace.ironsspellbooks.entity.spells.target_area.TargetedAreaEntity;
import io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

public final class ThunderstruckRuntime {
    public static final String SPELL_ID = HeroicSpiritSpell.MODID + ":thunderstruck";
    private static final int STAGE_ONE_TICKS = 20 * 4;
    private static final int ASCEND_TICKS = 15;
    private static final int TELEPORT_DELAY_AFTER_INVIS_TICKS = 1;
    private static final int STAGE_ONE_VOLT_STRIKE_TICKS = ASCEND_TICKS - 1;
    private static final float TARGET_AREA_RADIUS = 8.0f;
    private static final int TARGET_AREA_COLOR = 0x6EDBFF;
    private static final int ELECTRICITY_INTERVAL_TICKS = 2;
    private static final ResourceLocation SCALE_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(HeroicSpiritSpell.MODID, "thunderstruck_scale");
    private static final ResourceLocation STEP_HEIGHT_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(HeroicSpiritSpell.MODID, "thunderstruck_step_height");
    private static final ResourceLocation SPEED_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath(HeroicSpiritSpell.MODID, "thunderstruck_speed");
    private static final double SCALE_TARGET = 4.0;
    private static final double STEP_HEIGHT_TARGET = 8.0;
    private static final double SPEED_BONUS_MULTIPLIED_TOTAL = 1.0; // +100%
    private static final double DEFAULT_PLAYER_STEP_HEIGHT = 1.0;
    private static final int STAGE_TWO_STRIKE_COUNT = 16;
    private static final int STAGE_TWO_LOCK_TICKS = 20;
    private static final int GROUND_SCAN_MAX_DEPTH = 64;
    private static final int STAGE_TWO_SHOCKWAVE_COUNT = 4;
    private static final int STAGE_TWO_SHOCKWAVE_INTERVAL_TICKS = 5;
    private static final float STAGE_TWO_SHOCKWAVE_RADIUS = TARGET_AREA_RADIUS;
    private static final float STAGE_TWO_STRIKE_DAMAGE = 10f;
    private static final Vector3f STAGE_TWO_SHOCKWAVE_COLOR = new Vector3f(0.43f, 0.86f, 1.0f);
    private static final double STAGE_THREE_TELEPORT_UP = 8.0;
    private static final int STAGE_THREE_ELECTRONIZE_TICKS = 10;
    private static final int STAGE_THREE_CAMERA_RESTORE_TICKS = 10;
    private static final int STAGE_THREE_VOLT_STRIKE_TICKS = 20;
    private static final double STAGE_THREE_DIVE_SPEED = -2.8;
    private static final float STAGE_THREE_SHOCKWAVE_DAMAGE = 40f;
    private static final float STAGE_THREE_SHOCKWAVE_RADIUS = 8f;
    private static final double STAGE_THREE_SHOCKWAVE_HALF_HEIGHT = 4.0;
    private static final int STAGE_THREE_UNLUCK_TICKS = 20 * 5;
    private static final int STAGE_THREE_UNLUCK_AMPLIFIER = 3; // IV

    private static final String ACTIVE_KEY = HeroicSpiritSpell.MODID + ":thunderstruck_stage1_active";
    private static final String ORIGINAL_CAMERA_THIRD_PERSON_KEY = HeroicSpiritSpell.MODID + ":thunderstruck_orig_camera_is_third";
    private static final String AREA_ENTITY_ID_KEY = HeroicSpiritSpell.MODID + ":thunderstruck_area_entity_id";
    private static final String ORIGIN_X_KEY = HeroicSpiritSpell.MODID + ":thunderstruck_stage1_origin_x";
    private static final String ORIGIN_Y_KEY = HeroicSpiritSpell.MODID + ":thunderstruck_stage1_origin_y";
    private static final String ORIGIN_Z_KEY = HeroicSpiritSpell.MODID + ":thunderstruck_stage1_origin_z";
    private static final String ASCEND_REMAINING_KEY = HeroicSpiritSpell.MODID + ":thunderstruck_stage1_ascend_remaining";
    private static final String INVIS_APPLIED_KEY = HeroicSpiritSpell.MODID + ":thunderstruck_stage1_invis_applied";
    private static final String TELEPORT_DELAY_KEY = HeroicSpiritSpell.MODID + ":thunderstruck_stage1_teleport_delay";
    private static final String TRANSFORM_APPLIED_KEY = HeroicSpiritSpell.MODID + ":thunderstruck_stage1_transform_applied";
    private static final String STAGE_TWO_CENTER_X_KEY = HeroicSpiritSpell.MODID + ":thunderstruck_stage2_center_x";
    private static final String STAGE_TWO_CENTER_Y_KEY = HeroicSpiritSpell.MODID + ":thunderstruck_stage2_center_y";
    private static final String STAGE_TWO_CENTER_Z_KEY = HeroicSpiritSpell.MODID + ":thunderstruck_stage2_center_z";
    private static final String STAGE_TWO_SHOCKWAVES_LEFT_KEY = HeroicSpiritSpell.MODID + ":thunderstruck_stage2_shockwaves_left";
    private static final String STAGE_TWO_SHOCKWAVE_COOLDOWN_KEY = HeroicSpiritSpell.MODID + ":thunderstruck_stage2_shockwave_cooldown";
    private static final String STAGE_TWO_LOCK_REMAINING_KEY = HeroicSpiritSpell.MODID + ":thunderstruck_stage2_lock_remaining";
    private static final String STAGE_TWO_LOCK_X_KEY = HeroicSpiritSpell.MODID + ":thunderstruck_stage2_lock_x";
    private static final String STAGE_TWO_LOCK_Y_KEY = HeroicSpiritSpell.MODID + ":thunderstruck_stage2_lock_y";
    private static final String STAGE_TWO_LOCK_Z_KEY = HeroicSpiritSpell.MODID + ":thunderstruck_stage2_lock_z";
    private static final String STAGE_THREE_ACTIVE_KEY = HeroicSpiritSpell.MODID + ":thunderstruck_stage3_active";
    private static final String STAGE_THREE_CAMERA_TICKS_KEY = HeroicSpiritSpell.MODID + ":thunderstruck_stage3_camera_ticks";
    private static final String STAGE_THREE_CENTER_X_KEY = HeroicSpiritSpell.MODID + ":thunderstruck_stage3_center_x";
    private static final String STAGE_THREE_CENTER_Y_KEY = HeroicSpiritSpell.MODID + ":thunderstruck_stage3_center_y";
    private static final String STAGE_THREE_CENTER_Z_KEY = HeroicSpiritSpell.MODID + ":thunderstruck_stage3_center_z";
    private static final ResourceLocation TRUE_INVIS_ID = ResourceLocation.parse("irons_spellbooks:true_invisibility");
    private static final ResourceLocation VOLT_STRIKE_ID = ResourceLocation.parse("irons_spellbooks:volt_strike");

    private ThunderstruckRuntime() {
    }

    public static void startStageOne(ServerPlayer player) {
        player.getPersistentData().putBoolean(ACTIVE_KEY, true);
        player.getPersistentData().putDouble(ORIGIN_X_KEY, player.getX());
        player.getPersistentData().putDouble(ORIGIN_Y_KEY, player.getY());
        player.getPersistentData().putDouble(ORIGIN_Z_KEY, player.getZ());
        player.getPersistentData().putInt(ASCEND_REMAINING_KEY, ASCEND_TICKS);
        player.getPersistentData().putBoolean(INVIS_APPLIED_KEY, false);
        player.getPersistentData().putInt(TELEPORT_DELAY_KEY, -1);
        player.getPersistentData().putBoolean(TRANSFORM_APPLIED_KEY, false);
        PacketDistributor.sendToPlayer(player, new ThunderstruckCameraPayload(true, true, false));

        player.addEffect(new MobEffectInstance(ModEffects.ELECTRONIZE, STAGE_ONE_TICKS, 0, false, false, false));
        Holder<MobEffect> voltStrike = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                .getHolder(VOLT_STRIKE_ID)
                .orElse(null);
        if (voltStrike != null) {
            player.addEffect(new MobEffectInstance(
                    voltStrike,
                    STAGE_ONE_VOLT_STRIKE_TICKS,
                    0,
                    false,
                    false,
                    false
            ));
        }

        applyInitialUpwardBurst(player);
    }

    public static void tick(ServerPlayer player) {
        tickStageThreeCameraRestore(player);
        tickStageThree(player);
        tickStageTwoMovementLock(player);
        tickStageTwoShockwaves(player);
        if (!isStageOneActive(player)) {
            return;
        }
        if (!player.hasEffect(ModEffects.ELECTRONIZE)) {
            triggerStageTwo(player, true);
            return;
        }
        tickAscendAndTransform(player);
        if (player.getPersistentData().getBoolean(INVIS_APPLIED_KEY)) {
            keepSelfOnlyTargetAreaAlive(player);
        }
        if ((player.tickCount % ELECTRICITY_INTERVAL_TICKS) == 0) {
            spawnFootElectricity(player);
        }
        player.invulnerableTime = 20;
        player.fallDistance = 0f;
    }

    public static boolean isStageOneActive(Player player) {
        return player.getPersistentData().getBoolean(ACTIVE_KEY);
    }

    public static void triggerStageTwo(ServerPlayer player) {
        triggerStageTwo(player, false);
    }

    public static void triggerStageTwo(ServerPlayer player, boolean playCastAction) {
        if (!isStageOneActive(player)) {
            return;
        }
        // Stage-2 area/shockwaves must stay on ground at the pre-teleport location.
        Vec3 center = getGroundAnchorPos(player);
        makeTargetAreaVisibleToTeam(player, center);
        startStageTwoShockwaves(player, center);
        spawnDistributedLightningStrikes(player, center);
        applyStageTwoTransition(player);
        startStageTwoMovementLock(player);
        removeStageOneEffects(player);
        if (playCastAction) {
            HeroicSpiritSpell.LOGGER.info("Thunderstruck auto stage2 cast action triggered for {}", player.getGameProfile().getName());
            player.swing(InteractionHand.MAIN_HAND);
            PacketDistributor.sendToPlayer(player, new ThunderstruckStage2CastAnimPayload());
        }
        clearStageOneFlagsForStageTwo(player);
    }

    public static void markOriginalCameraIsThirdPerson(Player player, boolean isThirdPerson) {
        player.getPersistentData().putBoolean(ORIGINAL_CAMERA_THIRD_PERSON_KEY, isThirdPerson);
    }

    public static boolean wasOriginalCameraThirdPerson(Player player) {
        return player.getPersistentData().getBoolean(ORIGINAL_CAMERA_THIRD_PERSON_KEY);
    }

    public static void clear(ServerPlayer player) {
        discardTargetArea(player);
        removeStageOneSizeAndStepAttributes(player);
        player.getPersistentData().remove(ACTIVE_KEY);
        player.getPersistentData().remove(ORIGINAL_CAMERA_THIRD_PERSON_KEY);
        player.getPersistentData().remove(AREA_ENTITY_ID_KEY);
        player.getPersistentData().remove(ORIGIN_X_KEY);
        player.getPersistentData().remove(ORIGIN_Y_KEY);
        player.getPersistentData().remove(ORIGIN_Z_KEY);
        player.getPersistentData().remove(ASCEND_REMAINING_KEY);
        player.getPersistentData().remove(INVIS_APPLIED_KEY);
        player.getPersistentData().remove(TELEPORT_DELAY_KEY);
        player.getPersistentData().remove(TRANSFORM_APPLIED_KEY);
    }

    private static void removeStageOneEffects(ServerPlayer player) {
        player.removeEffect(ModEffects.ELECTRONIZE);
        Holder<MobEffect> trueInvis = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                .getHolder(TRUE_INVIS_ID)
                .orElse(null);
        if (trueInvis != null) {
            player.removeEffect(trueInvis);
        }
    }

    private static void applyInitialUpwardBurst(ServerPlayer player) {
        Vec3 upwardImpulse = new Vec3(0.0, 1.6, 0.0);
        player.hasImpulse = true;
        player.setDeltaMovement(
                player.getDeltaMovement().x * 0.2,
                upwardImpulse.y,
                player.getDeltaMovement().z * 0.2
        );
        player.hurtMarked = true;
    }

    private static void tickAscendAndTransform(ServerPlayer player) {
        int remaining = Math.max(0, player.getPersistentData().getInt(ASCEND_REMAINING_KEY));
        if (remaining > 0) {
            player.getPersistentData().putInt(ASCEND_REMAINING_KEY, remaining - 1);
            return;
        }
        if (!player.getPersistentData().getBoolean(INVIS_APPLIED_KEY)) {
            player.getPersistentData().putBoolean(INVIS_APPLIED_KEY, true);
            player.getPersistentData().putInt(TELEPORT_DELAY_KEY, TELEPORT_DELAY_AFTER_INVIS_TICKS);
            Holder<MobEffect> trueInvis = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                    .getHolder(TRUE_INVIS_ID)
                    .orElse(null);
            if (trueInvis != null) {
                player.addEffect(new MobEffectInstance(trueInvis, STAGE_ONE_TICKS, 0, false, false, false));
            }
            return;
        }

        int teleportDelay = player.getPersistentData().getInt(TELEPORT_DELAY_KEY);
        if (teleportDelay > 0) {
            player.getPersistentData().putInt(TELEPORT_DELAY_KEY, teleportDelay - 1);
            return;
        }

        if (!player.getPersistentData().getBoolean(TRANSFORM_APPLIED_KEY)) {
            player.getPersistentData().putBoolean(TRANSFORM_APPLIED_KEY, true);
            player.teleportTo(
                    player.getPersistentData().getDouble(ORIGIN_X_KEY),
                    player.getPersistentData().getDouble(ORIGIN_Y_KEY),
                    player.getPersistentData().getDouble(ORIGIN_Z_KEY)
            );
            player.setDeltaMovement(Vec3.ZERO);
            player.hurtMarked = true;
            applyStageOneSizeAndStepAttributes(player);
        }
    }

    private static void applyStageOneSizeAndStepAttributes(ServerPlayer player) {
        AttributeInstance scale = player.getAttribute(Attributes.SCALE);
        if (scale != null && scale.getModifier(SCALE_MODIFIER_ID) == null) {
            scale.addTransientModifier(new AttributeModifier(
                    SCALE_MODIFIER_ID,
                    SCALE_TARGET - 1.0,
                    AttributeModifier.Operation.ADD_VALUE
            ));
        }
        AttributeInstance stepHeight = player.getAttribute(Attributes.STEP_HEIGHT);
        if (stepHeight != null && stepHeight.getModifier(STEP_HEIGHT_MODIFIER_ID) == null) {
            stepHeight.addTransientModifier(new AttributeModifier(
                    STEP_HEIGHT_MODIFIER_ID,
                    STEP_HEIGHT_TARGET - DEFAULT_PLAYER_STEP_HEIGHT,
                    AttributeModifier.Operation.ADD_VALUE
            ));
        }
        AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveSpeed != null && moveSpeed.getModifier(SPEED_MODIFIER_ID) == null) {
            moveSpeed.addTransientModifier(new AttributeModifier(
                    SPEED_MODIFIER_ID,
                    SPEED_BONUS_MULTIPLIED_TOTAL,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            ));
        }
    }

    private static void removeStageOneSizeAndStepAttributes(ServerPlayer player) {
        AttributeInstance scale = player.getAttribute(Attributes.SCALE);
        if (scale != null) {
            scale.removeModifier(SCALE_MODIFIER_ID);
        }
        AttributeInstance stepHeight = player.getAttribute(Attributes.STEP_HEIGHT);
        if (stepHeight != null) {
            stepHeight.removeModifier(STEP_HEIGHT_MODIFIER_ID);
        }
        AttributeInstance moveSpeed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (moveSpeed != null) {
            moveSpeed.removeModifier(SPEED_MODIFIER_ID);
        }
    }

    private static void spawnFootElectricity(ServerPlayer player) {
        Vec3 p = player.position().add(0.0, 0.06, 0.0);
        MagicManager.spawnParticles(
                player.level(),
                ParticleHelper.ELECTRICITY,
                p.x, p.y, p.z,
                3,
                0.18, 0.02, 0.18,
                0.03,
                false
        );
    }

    private static void makeTargetAreaVisibleToTeam(ServerPlayer caster, Vec3 center) {
        ServerLevel level = caster.serverLevel();
        int keepTicks = STAGE_TWO_SHOCKWAVE_INTERVAL_TICKS * STAGE_TWO_SHOCKWAVE_COUNT + 20;
        // Ownerless area keeps a fixed center and will not follow a player into the air.
        TargetedAreaEntity area = TargetedAreaEntity.createTargetAreaEntity(
                level,
                center,
                TARGET_AREA_RADIUS,
                TARGET_AREA_COLOR
        );
        area.setShouldFade(false);
        area.setDuration(keepTicks);
        level.addFreshEntity(area);
    }

    private static void startStageTwoShockwaves(ServerPlayer player, Vec3 center) {
        player.getPersistentData().putDouble(STAGE_TWO_CENTER_X_KEY, center.x);
        player.getPersistentData().putDouble(STAGE_TWO_CENTER_Y_KEY, center.y);
        player.getPersistentData().putDouble(STAGE_TWO_CENTER_Z_KEY, center.z);
        player.getPersistentData().putInt(STAGE_TWO_SHOCKWAVES_LEFT_KEY, STAGE_TWO_SHOCKWAVE_COUNT);
        player.getPersistentData().putInt(STAGE_TWO_SHOCKWAVE_COOLDOWN_KEY, 0);
    }

    private static void tickStageTwoShockwaves(ServerPlayer player) {
        int wavesLeft = player.getPersistentData().getInt(STAGE_TWO_SHOCKWAVES_LEFT_KEY);
        if (wavesLeft <= 0) {
            return;
        }
        int cooldown = player.getPersistentData().getInt(STAGE_TWO_SHOCKWAVE_COOLDOWN_KEY);
        if (cooldown > 0) {
            player.getPersistentData().putInt(STAGE_TWO_SHOCKWAVE_COOLDOWN_KEY, cooldown - 1);
            return;
        }
        Vec3 center = new Vec3(
                player.getPersistentData().getDouble(STAGE_TWO_CENTER_X_KEY),
                player.getPersistentData().getDouble(STAGE_TWO_CENTER_Y_KEY),
                player.getPersistentData().getDouble(STAGE_TWO_CENTER_Z_KEY)
        );
        MagicManager.spawnParticles(
                player.level(),
                new BlastwaveParticleOptions(STAGE_TWO_SHOCKWAVE_COLOR, STAGE_TWO_SHOCKWAVE_RADIUS),
                center.x, center.y + 0.16, center.z,
                1, 0, 0, 0, 0,
                true
        );
        wavesLeft--;
        if (wavesLeft <= 0) {
            player.getPersistentData().remove(STAGE_TWO_SHOCKWAVES_LEFT_KEY);
            player.getPersistentData().remove(STAGE_TWO_SHOCKWAVE_COOLDOWN_KEY);
            player.getPersistentData().remove(STAGE_TWO_CENTER_X_KEY);
            player.getPersistentData().remove(STAGE_TWO_CENTER_Y_KEY);
            player.getPersistentData().remove(STAGE_TWO_CENTER_Z_KEY);
        } else {
            player.getPersistentData().putInt(STAGE_TWO_SHOCKWAVES_LEFT_KEY, wavesLeft);
            player.getPersistentData().putInt(STAGE_TWO_SHOCKWAVE_COOLDOWN_KEY, STAGE_TWO_SHOCKWAVE_INTERVAL_TICKS);
        }
    }

    private static void startStageTwoMovementLock(ServerPlayer player) {
        player.getPersistentData().putInt(STAGE_TWO_LOCK_REMAINING_KEY, STAGE_TWO_LOCK_TICKS);
        player.getPersistentData().putDouble(STAGE_TWO_LOCK_X_KEY, player.getX());
        player.getPersistentData().putDouble(STAGE_TWO_LOCK_Y_KEY, player.getY());
        player.getPersistentData().putDouble(STAGE_TWO_LOCK_Z_KEY, player.getZ());
    }

    private static void tickStageTwoMovementLock(ServerPlayer player) {
        int remaining = player.getPersistentData().getInt(STAGE_TWO_LOCK_REMAINING_KEY);
        if (remaining <= 0) {
            return;
        }
        player.noPhysics = true;
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0f;
        double x = player.getPersistentData().getDouble(STAGE_TWO_LOCK_X_KEY);
        double y = player.getPersistentData().getDouble(STAGE_TWO_LOCK_Y_KEY);
        double z = player.getPersistentData().getDouble(STAGE_TWO_LOCK_Z_KEY);
        player.teleportTo(x, y, z);
        player.hurtMarked = true;
        player.getPersistentData().putInt(STAGE_TWO_LOCK_REMAINING_KEY, remaining - 1);
        if (remaining - 1 <= 0) {
            Vec3 lockPos = new Vec3(x, y, z);
            player.getPersistentData().remove(STAGE_TWO_LOCK_REMAINING_KEY);
            player.getPersistentData().remove(STAGE_TWO_LOCK_X_KEY);
            player.getPersistentData().remove(STAGE_TWO_LOCK_Y_KEY);
            player.getPersistentData().remove(STAGE_TWO_LOCK_Z_KEY);
            if (!player.isSpectator()
                    && !player.hasEffect(ModEffects.ELECTRONIZE)
                    && !player.hasEffect(ModEffects.INCORPOREITY)) {
                player.noPhysics = false;
            }
            beginStageThree(player, lockPos);
        }
    }

    private static void beginStageThree(ServerPlayer player, Vec3 lockPos) {
        player.noPhysics = false;
        player.addEffect(new MobEffectInstance(
                ModEffects.ELECTRONIZE,
                STAGE_THREE_ELECTRONIZE_TICKS,
                0,
                false,
                false,
                false
        ));
        Holder<MobEffect> voltStrike = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                .getHolder(VOLT_STRIKE_ID)
                .orElse(null);
        if (voltStrike != null) {
            player.addEffect(new MobEffectInstance(
                    voltStrike,
                    STAGE_THREE_VOLT_STRIKE_TICKS,
                    0,
                    false,
                    false,
                    false
            ));
        }
        player.getPersistentData().putBoolean(STAGE_THREE_ACTIVE_KEY, true);
        player.getPersistentData().putInt(STAGE_THREE_CAMERA_TICKS_KEY, STAGE_THREE_CAMERA_RESTORE_TICKS);
        player.getPersistentData().putDouble(STAGE_THREE_CENTER_X_KEY, player.getPersistentData().contains(STAGE_TWO_CENTER_X_KEY) ? player.getPersistentData().getDouble(STAGE_TWO_CENTER_X_KEY) : lockPos.x);
        player.getPersistentData().putDouble(STAGE_THREE_CENTER_Y_KEY, player.getPersistentData().contains(STAGE_TWO_CENTER_Y_KEY) ? player.getPersistentData().getDouble(STAGE_TWO_CENTER_Y_KEY) : lockPos.y);
        player.getPersistentData().putDouble(STAGE_THREE_CENTER_Z_KEY, player.getPersistentData().contains(STAGE_TWO_CENTER_Z_KEY) ? player.getPersistentData().getDouble(STAGE_TWO_CENTER_Z_KEY) : lockPos.z);
    }

    private static void applyStageTwoTransition(ServerPlayer player) {
        removeStageOneSizeAndStepAttributes(player);
        Vec3 pos = player.position();
        player.teleportTo(pos.x, pos.y + STAGE_THREE_TELEPORT_UP, pos.z);
        player.setDeltaMovement(Vec3.ZERO);
        player.hurtMarked = true;
    }

    private static void tickStageThree(ServerPlayer player) {
        if (!player.getPersistentData().getBoolean(STAGE_THREE_ACTIVE_KEY)) {
            return;
        }
        player.fallDistance = 0f;
        player.setDeltaMovement(0.0, STAGE_THREE_DIVE_SPEED, 0.0);
        player.hurtMarked = true;

        if (player.onGround()) {
            executeStageThreeLanding(player);
            // Failsafe: make sure temporary stage-1 attribute modifiers are always removed.
            removeStageOneSizeAndStepAttributes(player);
            player.getPersistentData().remove(STAGE_THREE_ACTIVE_KEY);
            player.getPersistentData().remove(STAGE_THREE_CENTER_X_KEY);
            player.getPersistentData().remove(STAGE_THREE_CENTER_Y_KEY);
            player.getPersistentData().remove(STAGE_THREE_CENTER_Z_KEY);
        }
    }

    private static void tickStageThreeCameraRestore(ServerPlayer player) {
        int cameraTicks = player.getPersistentData().getInt(STAGE_THREE_CAMERA_TICKS_KEY);
        if (cameraTicks <= 0) {
            return;
        }
        player.getPersistentData().putInt(STAGE_THREE_CAMERA_TICKS_KEY, cameraTicks - 1);
        if (cameraTicks - 1 == 0) {
            PacketDistributor.sendToPlayer(player, new ThunderstruckCameraPayload(false, false, true));
            player.getPersistentData().remove(STAGE_THREE_CAMERA_TICKS_KEY);
        }
    }

    private static void executeStageThreeLanding(ServerPlayer player) {
        Vec3 shockwaveCenter = new Vec3(player.getX(), player.getBoundingBox().minY, player.getZ());
        spawnVisualLightningBolt(player);
        applyStageThreeShockwave(player, shockwaveCenter);
        spawnStageThreeAreaElectricity(player, shockwaveCenter);
    }

    private static void spawnVisualLightningBolt(ServerPlayer player) {
        LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, player.level());
        bolt.setVisualOnly(true);
        bolt.setDamage(0f);
        bolt.moveTo(player.getX(), player.getBoundingBox().minY, player.getZ(), 0f, 0f);
        player.level().addFreshEntity(bolt);
    }

    private static void applyStageThreeShockwave(ServerPlayer player, Vec3 center) {
        double halfHeight = STAGE_THREE_SHOCKWAVE_HALF_HEIGHT;
        double r2 = STAGE_THREE_SHOCKWAVE_RADIUS * STAGE_THREE_SHOCKWAVE_RADIUS;
        AABB box = new AABB(center, center).inflate(
                STAGE_THREE_SHOCKWAVE_RADIUS,
                halfHeight,
                STAGE_THREE_SHOCKWAVE_RADIUS
        );
        double minY = center.y - halfHeight;
        double maxY = center.y + halfHeight;
        for (LivingEntity target : player.level().getEntitiesOfClass(
                LivingEntity.class,
                box,
                entity -> entity != player
                        && entity.isAlive()
                        && !entity.isSpectator()
                        && !DamageSources.isFriendlyFireBetween(entity, player)
        )) {
            AABB targetBox = target.getBoundingBox();
            if (targetBox.maxY < minY || targetBox.minY > maxY) {
                continue;
            }
            double dx = target.getX() - center.x;
            double dz = target.getZ() - center.z;
            double horizontalDistanceSq = dx * dx + dz * dz;
            if (horizontalDistanceSq > r2) {
                continue;
            }
            float shockwaveDamage = (float) (STAGE_THREE_SHOCKWAVE_DAMAGE * getThunderstruckPowerMultiplier(player));
            var thunderstruckSource = ModSpells.THUNDERSTRUCK.get().getDamageSource(player);
            if (thunderstruckSource instanceof SpellDamageSource spellDamageSource) {
                spellDamageSource.setIFrames(0);
            }
            // Stage-2 lightning can leave the target in hurt i-frames; clear them so stage-3 impact always lands.
            target.invulnerableTime = 0;
            DamageSources.applyDamage(
                    target,
                    shockwaveDamage,
                    thunderstruckSource
            );
            target.addEffect(new MobEffectInstance(
                    MobEffects.UNLUCK,
                    STAGE_THREE_UNLUCK_TICKS,
                    STAGE_THREE_UNLUCK_AMPLIFIER,
                    false,
                    true,
                    true
            ));
        }
    }

    private static void spawnStageThreeAreaElectricity(ServerPlayer player, Vec3 center) {
        for (int i = 0; i < 8; i++) {
            MagicManager.spawnParticles(
                    player.level(),
                    ParticleHelper.ELECTRICITY,
                    center.x, center.y + 0.15, center.z,
                    25,
                    TARGET_AREA_RADIUS * 0.65,
                    0.35,
                    TARGET_AREA_RADIUS * 0.65,
                    0.2,
                    false
            );
        }
    }

    private static void spawnDistributedLightningStrikes(ServerPlayer caster, Vec3 center) {
        ServerLevel level = caster.serverLevel();
        List<Vec3> positions = new ArrayList<>();

        LivingEntity prioritized = findPrioritizedEnemy(level, caster, center, TARGET_AREA_RADIUS);
        if (prioritized != null) {
            positions.add(new Vec3(prioritized.getX(), center.y, prioritized.getZ()));
        }

        int attempts = 0;
        while (positions.size() < STAGE_TWO_STRIKE_COUNT && attempts < 500) {
            attempts++;
            Vec3 p = samplePointInCircle(center, TARGET_AREA_RADIUS);
            if (isFarEnough(p, positions, 2.0)) {
                positions.add(p);
            }
        }

        while (positions.size() < STAGE_TWO_STRIKE_COUNT) {
            positions.add(samplePointInCircle(center, TARGET_AREA_RADIUS));
        }

        for (Vec3 p : positions) {
            LightningStrike strike = new LightningStrike(level);
            strike.setOwner(caster);
            strike.setDamage((float) (STAGE_TWO_STRIKE_DAMAGE * getThunderstruckPowerMultiplier(caster)));
            strike.setPos(p.x, center.y, p.z);
            level.addFreshEntity(strike);
        }
    }

    private static LivingEntity findPrioritizedEnemy(ServerLevel level, ServerPlayer caster, Vec3 center, float radius) {
        AABB box = new AABB(center, center).inflate(radius, 4.0, radius);
        double radiusSq = radius * radius;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (e == caster || !e.isAlive() || e.isSpectator() || e.isAlliedTo(caster) || caster.isAlliedTo(e)) {
                continue;
            }
            if (new Vec3(e.getX(), center.y, e.getZ()).distanceToSqr(center) <= radiusSq) {
                return e;
            }
        }
        return null;
    }

    private static Vec3 samplePointInCircle(Vec3 center, float radius) {
        double angle = Math.PI * 2.0 * Math.random();
        double dist = radius * Math.sqrt(Math.random());
        return new Vec3(center.x + Math.cos(angle) * dist, center.y, center.z + Math.sin(angle) * dist);
    }

    private static boolean isFarEnough(Vec3 candidate, List<Vec3> existing, double minDistance) {
        double minSq = minDistance * minDistance;
        for (Vec3 p : existing) {
            if (p.distanceToSqr(candidate) < minSq) {
                return false;
            }
        }
        return true;
    }

    private static void createOrRefreshSelfOnlyTargetArea(ServerPlayer player) {
        TargetedAreaEntity area = getArea(player);
        if (area == null || area.isRemoved()) {
            area = TargetedAreaEntity.createTargetAreaEntity(
                    player.serverLevel(),
                    getFootFollowPos(player),
                    TARGET_AREA_RADIUS,
                    TARGET_AREA_COLOR,
                    player
            );
            area.setDuration(STAGE_ONE_TICKS + 10);
            area.setShouldFade(false);
            player.serverLevel().addFreshEntity(area);
            player.getPersistentData().putInt(AREA_ENTITY_ID_KEY, area.getId());
            return;
        }
        area.setDuration(STAGE_ONE_TICKS + 10);
    }

    private static void clearStageOneFlagsForStageTwo(ServerPlayer player) {
        discardTargetArea(player);
        player.getPersistentData().remove(ACTIVE_KEY);
        player.getPersistentData().remove(AREA_ENTITY_ID_KEY);
        player.getPersistentData().remove(ORIGIN_X_KEY);
        player.getPersistentData().remove(ORIGIN_Y_KEY);
        player.getPersistentData().remove(ORIGIN_Z_KEY);
        player.getPersistentData().remove(ASCEND_REMAINING_KEY);
        player.getPersistentData().remove(INVIS_APPLIED_KEY);
        player.getPersistentData().remove(TELEPORT_DELAY_KEY);
        player.getPersistentData().remove(TRANSFORM_APPLIED_KEY);
    }

    private static void keepSelfOnlyTargetAreaAlive(ServerPlayer player) {
        TargetedAreaEntity area = getArea(player);
        if (area == null || area.isRemoved()) {
            createOrRefreshSelfOnlyTargetArea(player);
            return;
        }
        area.setPos(getFootFollowPos(player));
        area.setDuration(10);
    }

    private static Vec3 getFootFollowPos(ServerPlayer player) {
        double y = player.getBoundingBox().minY + 0.05;
        return new Vec3(player.getX(), y, player.getZ());
    }

    private static Vec3 getGroundAnchorPos(ServerPlayer player) {
        double x = player.getX();
        double z = player.getZ();
        double startY = player.getBoundingBox().minY;
        BlockPos base = BlockPos.containing(x, startY, z);
        for (int i = 0; i <= GROUND_SCAN_MAX_DEPTH; i++) {
            BlockPos pos = base.below(i);
            BlockState state = player.level().getBlockState(pos);
            if (state.blocksMotion()) {
                return new Vec3(x, pos.getY() + 1.05, z);
            }
        }
        return new Vec3(x, startY, z);
    }

    private static TargetedAreaEntity getArea(ServerPlayer player) {
        if (!player.getPersistentData().contains(AREA_ENTITY_ID_KEY)) {
            return null;
        }
        Entity entity = player.level().getEntity(player.getPersistentData().getInt(AREA_ENTITY_ID_KEY));
        if (entity instanceof TargetedAreaEntity area) {
            return area;
        }
        return null;
    }

    private static void discardTargetArea(ServerPlayer player) {
        TargetedAreaEntity area = getArea(player);
        if (area != null && !area.isRemoved()) {
            area.discard();
        }
    }

    private static double getThunderstruckPowerMultiplier(ServerPlayer caster) {
        var spell = ModSpells.THUNDERSTRUCK.get();
        double baseline = spell.getSpellPower(1, null);
        if (baseline <= 1.0e-6) {
            return 1.0;
        }
        return Math.max(0.0, spell.getSpellPower(1, caster) / baseline);
    }
}
