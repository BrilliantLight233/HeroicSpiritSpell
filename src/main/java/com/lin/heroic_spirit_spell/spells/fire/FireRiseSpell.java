package com.lin.heroic_spirit_spell.spells.fire;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.ICastData;
import io.redspace.ironsspellbooks.api.spells.ICastDataSerializable;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.capabilities.magic.ImpulseCastData;
import io.redspace.ironsspellbooks.player.SpinAttackType;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.registries.SoundRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

import net.minecraft.sounds.SoundEvent;

/**
 * Fire Rise: upward dash matching Irons {@code BurningDashSpell} at 5 contact damage (spell power 0 to multiplier),
 * then Slow Falling after the dash duration (same as burning_dash effect length).
 */
public class FireRiseSpell extends AbstractSpell {

    private static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(HeroicSpiritSpell.MODID, "fire_rise");

    /** Same as BurningDash: (15 + spellPower) / 12; 0-level equivalent uses 0 spell power. */
    private static final float BURNING_DASH_SPELL_POWER_FOR_EQUIV = 0f;
    /** Matches BurningDash#getDamage at spell power 0: 5 + 0 */
    private static final int BURNING_DASH_CONTACT_DAMAGE_AMP = 5;
    /** Irons BurningDash: BURNING_DASH effect duration */
    private static final int BURNING_DASH_STATE_TICKS = 15;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.COMMON)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(3f)
            .build();

    public FireRiseSpell() {
        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 0;
        this.castTime = 0;
        this.baseManaCost = 0;
        this.manaCostPerLevel = 0;
    }

    @Override
    public CastType getCastType() {
        return CastType.INSTANT;
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
                Component.translatable("ui.irons_spellbooks.damage", BURNING_DASH_CONTACT_DAMAGE_AMP),
                Component.translatable("spell.heroic_spirit_spell.fire_rise.info_slow_falling", 6)
        );
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        return Optional.of(SoundRegistry.FIRE_CAST.get());
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.TOUCH_GROUND_ANIMATION;
    }

    @Override
    public void onClientCast(Level level, int spellLevel, LivingEntity entity, ICastData castData) {
        if (castData instanceof ImpulseCastData bdcd) {
            entity.hasImpulse = bdcd.hasImpulse;
            entity.setDeltaMovement(entity.getDeltaMovement().add(bdcd.x, bdcd.y, bdcd.z));
        }
        super.onClientCast(level, spellLevel, entity, castData);
    }

    @Override
    public ICastDataSerializable getEmptyCastData() {
        return new ImpulseCastData();
    }

    @Override
    public void onCast(Level world, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        entity.hasImpulse = true;
        float multiplier = (15f + BURNING_DASH_SPELL_POWER_FOR_EQUIV) / 12f;

        Vec3 forward = new Vec3(0.0, 1.0, 0.0);
        Vec3 vec = forward.multiply(3.0, 1.0, 3.0).normalize().add(0.0, 0.25, 0.0).scale(multiplier);
        if (entity.onGround()) {
            entity.setPos(entity.position().add(0.0, 1.5, 0.0));
            vec = vec.add(0.0, 0.25, 0.0);
        }

        playerMagicData.setAdditionalCastData(new ImpulseCastData(
                (float) vec.x, (float) vec.y, (float) vec.z, true));

        entity.setDeltaMovement(new Vec3(
                Mth.lerp(0.75f, entity.getDeltaMovement().x, vec.x),
                Mth.lerp(0.75f, entity.getDeltaMovement().y, vec.y),
                Mth.lerp(0.75f, entity.getDeltaMovement().z, vec.z)));
        entity.hurtMarked = true;

        entity.addEffect(new MobEffectInstance(
                MobEffectRegistry.BURNING_DASH,
                BURNING_DASH_STATE_TICKS,
                BURNING_DASH_CONTACT_DAMAGE_AMP,
                false,
                false,
                false));
        entity.invulnerableTime = 20;
        playerMagicData.getSyncedData().setSpinAttackType(SpinAttackType.FIRE);

        entity.getPersistentData().putInt(FireRiseAscensionDelay.PERSIST_KEY, BURNING_DASH_STATE_TICKS);

        super.onCast(world, spellLevel, entity, castSource, playerMagicData);
    }
}
