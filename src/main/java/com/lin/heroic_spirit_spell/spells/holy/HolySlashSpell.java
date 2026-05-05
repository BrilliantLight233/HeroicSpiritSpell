package com.lin.heroic_spirit_spell.spells.holy;

import java.util.List;
import java.util.Optional;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.entity.spells.HolySlashProjectile;

import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * 神圣斩击 Holy Slash
 * 参考 BloodSlashSpell 实现
 */
public class HolySlashSpell extends AbstractSpell {

    private static final ResourceLocation SPELL_ID =
            ResourceLocation.parse(HeroicSpiritSpell.MODID + ":holy_slash");
    /**
     * 对应 sounds.json 的 cast.generic.blood -> generic/blood_cast.ogg
     */
    private static final ResourceLocation HOLY_SLASH_CAST_SOUND_ID =
            ResourceLocation.parse("irons_spellbooks:cast.generic.blood");

    // 基础配置：学派、冷却、稀有度、最大等级等
    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.UNCOMMON)
            .setSchoolResource(SchoolRegistry.HOLY_RESOURCE)
            .setMaxLevel(10)
            .setCooldownSeconds(3)
            .build();

    public HolySlashSpell() {
        // 伤害 = 5 + 每级 5
        this.baseSpellPower = 5;
        this.spellPowerPerLevel = 5;
        // 瞬发、无蓝耗
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
        // 用自己模组的命名空间，方便自定义图标 holy_slash.png
        return SPELL_ID;
    }

    @Override
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        // UI 展示伤害：纯法术强度
        float spellPower = getSpellPower(spellLevel, caster);
        MutableComponent component = Component.translatable(
                "ui.irons_spellbooks.damage",
                Utils.stringTruncation(spellPower, 2)
        );
        return List.of(component);
    }

    @Override
    public Optional<SoundEvent> getCastStartSound() {
        // 施法起手播放 blood_cast.ogg
        return BuiltInRegistries.SOUND_EVENT.getOptional(HOLY_SLASH_CAST_SOUND_ID);
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        // 与 blood_slash 一致使用斩击起手动画
        return SpellAnimations.SLASH_ANIMATION;
    }

    @Override
    public Optional<SoundEvent> getCastFinishSound() {
        return Optional.empty();
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity caster,
                       CastSource castSource, MagicData magicData) {
        if (level.isClientSide) {
            return;
        }

        // 计算伤害：5 + 每级 5（不叠加近战攻击）
        float damage = getSpellPower(spellLevel, caster);

        HolySlashProjectile projectile = new HolySlashProjectile(level, caster);
        // 从视线高度发射
        projectile.setPos(caster.getEyePosition().add(0.0, -0.25, 0.0));

        // 完全对齐 blood_slash：直接使用 lookAngle 发射
        projectile.shoot(caster.getLookAngle());

        projectile.setDamage(damage);
        level.addFreshEntity(projectile);

        super.onCast(level, spellLevel, caster, castSource, magicData);
    }
}