package com.lin.heroic_spirit_spell.registry;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.entity.spells.HolySlashProjectile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import net.minecraft.core.registries.Registries;

public class ModEntities {

    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, HeroicSpiritSpell.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<HolySlashProjectile>> HOLY_SLASH_PROJECTILE =
            ENTITIES.register("holy_slash_projectile",
                    () -> EntityType.Builder.<HolySlashProjectile>of(HolySlashProjectile::new, MobCategory.MISC)
                            .sized(1.0f, 0.25f)           // 和 Blood Slash 类似的宽度/高度
                            .clientTrackingRange(128)     // 高速法术建议大范围追踪
                            .updateInterval(1)
                            .build(HeroicSpiritSpell.MODID + ":holy_slash_projectile")
            );

    public static void register(IEventBus modEventBus) {
        ENTITIES.register(modEventBus);
    }
}