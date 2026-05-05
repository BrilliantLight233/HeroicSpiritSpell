package com.lin.heroic_spirit_spell.spells.fire;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.spells.target_area.TargetedAreaEntity;
import io.redspace.ironsspellbooks.network.casting.SyncTargetingDataPacket;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import io.redspace.ironsspellbooks.spells.TargetAreaCastData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public class SkyfireGuardSpell extends AbstractSpell {
    private static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(HeroicSpiritSpell.MODID, "skyfire_guard");

    private static final int CAST_TICKS = 25; // 1.25s
    private static final int MAX_SELECT_RANGE = 64;
    private static final double CONE_HALF_ANGLE_DEGREES = 15.0;
    private static final float TARGET_AREA_RADIUS = 8.0f;
    private static final int TARGET_AREA_COLOR = 0xFF8C00; // orange
    private static final int TARGET_AREA_KEEP_TICKS = 20 * 300;
    private static final int ABSORPTION_TICKS = 20 * 10;
    private static final int ABSORPTION_AMPLIFIER = 4; // V
    private static final double MID_PHASE_TARGET_HEIGHT = 16.0;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.LEGENDARY)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(5)
            .setCooldownSeconds(30)
            .build();

    public SkyfireGuardSpell() {
        this.castTime = CAST_TICKS;
        this.baseManaCost = 250;
        this.manaCostPerLevel = 0;
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 5;
    }

    @Override
    public CastType getCastType() {
        return CastType.LONG;
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
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_LONG_CAST;
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.FIRE_CAST.get());
    }

    @Override
    public Vector3f getTargetingColor() {
        return Utils.deconstructRGB(TARGET_AREA_COLOR);
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("spell.heroic_spirit_spell.skyfire_guard.info_range", MAX_SELECT_RANGE),
                Component.translatable("spell.heroic_spirit_spell.skyfire_guard.info_radius", (int) TARGET_AREA_RADIUS)
        );
    }

    @Override
    public boolean checkPreCastConditions(Level level, int spellLevel, LivingEntity caster, MagicData magicData) {
        LivingEntity target = selectBestAllyTarget(caster, MAX_SELECT_RANGE, CONE_HALF_ANGLE_DEGREES);
        if (target == null) {
            if (!level.isClientSide && caster instanceof ServerPlayer sp) {
                sp.connection.send(new ClientboundSetActionBarTextPacket(
                        Component.translatable("ui.irons_spellbooks.cast_error_target").withStyle(ChatFormatting.RED)));
            }
            return false;
        }

        Vec3 center = target.position();
        TargetedAreaEntity area = null;
        if (magicData.getAdditionalCastData() instanceof TargetAreaCastData tac) {
            area = tac.getCastingEntity();
            if (area != null && !area.isRemoved()) {
                area.setPos(center);
                area.setDuration(TARGET_AREA_KEEP_TICKS);
            }
        }
        if (area == null || area.isRemoved()) {
            // Use ownerless target area so all players (including enemies) can see the zone.
            area = TargetedAreaEntity.createTargetAreaEntity(level, center, TARGET_AREA_RADIUS, TARGET_AREA_COLOR);
            area.setDuration(TARGET_AREA_KEEP_TICKS);
            area.setShouldFade(false);
        }
        magicData.setAdditionalCastData(new TargetAreaCastData(center, area));

        if (!level.isClientSide && caster instanceof ServerPlayer sp) {
            sp.connection.send(new ClientboundSetActionBarTextPacket(
                    Component.translatable("spell.heroic_spirit_spell.skyfire_guard.target_selected",
                            target.getDisplayName().getString())
                            .withStyle(ChatFormatting.RED)));
            PacketDistributor.sendToPlayer(sp, new SyncTargetingDataPacket(target, this));
        }
        return true;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster, CastSource castSource, MagicData magicData) {
        if (level.isClientSide) {
            return;
        }
        Vec3 center = resolveAreaCenter(caster, magicData);
        double r2 = TARGET_AREA_RADIUS * TARGET_AREA_RADIUS;
        AABB areaBox = new AABB(center, center).inflate(TARGET_AREA_RADIUS, 4.0, TARGET_AREA_RADIUS);

        caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ABSORPTION_TICKS, ABSORPTION_AMPLIFIER, false, true, true));

        for (LivingEntity ally : level.getEntitiesOfClass(LivingEntity.class, areaBox)) {
            if (ally == caster || !isFriendly(caster, ally) || ally.position().distanceToSqr(center) > r2) {
                continue;
            }
            ally.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, ABSORPTION_TICKS, ABSORPTION_AMPLIFIER, false, true, true));
        }

        TargetedAreaEntity area = magicData.getAdditionalCastData() instanceof TargetAreaCastData tac ? tac.getCastingEntity() : null;
        SkyfireGuardMidflight.start(caster, center.add(0.0, MID_PHASE_TARGET_HEIGHT, 0.0), center, spellLevel, area);
        super.onCast(level, spellLevel, caster, castSource, magicData);
    }

    private static Vec3 resolveAreaCenter(LivingEntity caster, MagicData magicData) {
        if (magicData.getAdditionalCastData() instanceof TargetAreaCastData tac) {
            TargetedAreaEntity area = tac.getCastingEntity();
            if (area != null && !area.isRemoved()) {
                return area.position();
            }
            return tac.getCenter();
        }
        return caster.position();
    }

    private static LivingEntity selectBestAllyTarget(LivingEntity caster, double maxRange, double halfAngleDegrees) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle().normalize();
        double cosThreshold = Math.cos(Math.toRadians(halfAngleDegrees));
        AABB searchBox = caster.getBoundingBox().inflate(maxRange);

        return caster.level().getEntitiesOfClass(LivingEntity.class, searchBox).stream()
                .filter(candidate -> candidate != caster)
                .filter(candidate -> isFriendly(caster, candidate))
                .filter(candidate -> {
                    Vec3 to = candidate.getBoundingBox().getCenter().subtract(eye);
                    double dist2 = to.lengthSqr();
                    if (dist2 < 1e-6 || dist2 > maxRange * maxRange) {
                        return false;
                    }
                    double dot = look.dot(to.normalize());
                    return dot >= cosThreshold;
                })
                .min(Comparator
                        .comparingDouble((LivingEntity candidate) -> angleScore(caster, candidate))
                        .thenComparingDouble(candidate -> candidate.distanceToSqr(caster)))
                .orElse(null);
    }

    private static double angleScore(LivingEntity caster, LivingEntity candidate) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 to = candidate.getBoundingBox().getCenter().subtract(eye).normalize();
        return 1.0 - look.dot(to);
    }

    private static boolean isFriendly(LivingEntity caster, LivingEntity candidate) {
        return candidate.isAlliedTo(caster) || caster.isAlliedTo(candidate);
    }
}
