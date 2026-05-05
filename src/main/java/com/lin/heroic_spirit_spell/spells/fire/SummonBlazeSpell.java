package com.lin.heroic_spirit_spell.spells.fire;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.util.SummonBlazeRuntime;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SchoolRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.api.spells.SpellAnimations;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.api.util.AnimationHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Summon Blaze: five stationary blazes orbit the caster; flaming barrage triggers extra volley from them.
 */
public class SummonBlazeSpell extends AbstractSpell {

    private static final ResourceLocation SPELL_ID =
            ResourceLocation.fromNamespaceAndPath(HeroicSpiritSpell.MODID, "summon_blaze");

    private static final int CAST_TICKS = 30;
    private static final int BLAZE_COUNT = SummonBlazeRuntime.BLAZE_COUNT;

    private final DefaultConfig defaultConfig = new DefaultConfig()
            .setMinRarity(SpellRarity.RARE)
            .setSchoolResource(SchoolRegistry.FIRE_RESOURCE)
            .setMaxLevel(1)
            .setCooldownSeconds(30f)
            .build();

    public SummonBlazeSpell() {
        this.baseSpellPower = 0;
        this.spellPowerPerLevel = 0;
        this.castTime = CAST_TICKS;
        this.baseManaCost = 250;
        this.manaCostPerLevel = 0;
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
    public List<MutableComponent> getUniqueInfo(int spellLevel, LivingEntity caster) {
        return List.of(
                Component.translatable("spell.heroic_spirit_spell.summon_blaze.info_blazes", BLAZE_COUNT),
                Component.translatable("spell.heroic_spirit_spell.summon_blaze.info_duration"),
                Component.translatable("spell.heroic_spirit_spell.summon_blaze.info_barrage")
        );
    }

    @Override
    public AnimationHolder getCastStartAnimation() {
        return SpellAnimations.ANIMATION_LONG_CAST;
    }

    @Override
    public void onCast(Level level, int spellLevel, LivingEntity entity, CastSource castSource, MagicData playerMagicData) {
        if (level.isClientSide || !(entity instanceof ServerPlayer player)) {
            super.onCast(level, spellLevel, entity, castSource, playerMagicData);
            return;
        }
        ServerLevel sl = player.serverLevel();
        Blaze[] blazes = new Blaze[BLAZE_COUNT];
        for (int i = 0; i < BLAZE_COUNT; i++) {
            Blaze blaze = EntityType.BLAZE.create(sl);
            if (blaze == null) {
                continue;
            }
            Vec3 pos = SummonBlazeRuntime.slotPosition(player, i);
            blaze.moveTo(pos.x, pos.y, pos.z, player.getYRot(), 0f);
            blaze.setNoAi(true);
            blaze.setInvulnerable(true);
            blaze.setSilent(true);
            blaze.setPersistenceRequired();
            blaze.setCanPickUpLoot(false);
            sl.addFreshEntity(blaze);
            blazes[i] = blaze;
        }
        SummonBlazeRuntime.replace(player, blazes);
        super.onCast(level, spellLevel, entity, castSource, playerMagicData);
    }
}
