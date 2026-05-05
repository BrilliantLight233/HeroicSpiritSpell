package com.lin.heroic_spirit_spell.event;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.registry.ModEffects;
import com.lin.heroic_spirit_spell.util.ThunderstruckRuntime;
import io.redspace.ironsspellbooks.api.events.SpellPreCastEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = HeroicSpiritSpell.MODID)
public final class ThunderstruckEvents {
    private ThunderstruckEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        ThunderstruckRuntime.tick(player);
    }

    @SubscribeEvent
    public static void onAttackEntity(AttackEntityEvent event) {
        if (event.getEntity().hasEffect(ModEffects.ELECTRONIZE)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (event.getEntity().hasEffect(ModEffects.ELECTRONIZE)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity().hasEffect(ModEffects.ELECTRONIZE)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onSpellPreCast(SpellPreCastEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!player.hasEffect(ModEffects.ELECTRONIZE)) {
            return;
        }
        if (!ThunderstruckRuntime.SPELL_ID.equals(event.getSpellId())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onLivingDamagePre(LivingDamageEvent.Pre event) {
        if (event.getEntity().hasEffect(ModEffects.ELECTRONIZE)) {
            event.setNewDamage(0f);
        }
    }

    @SubscribeEvent
    public static void onLivingJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player player)) {
            return;
        }
        if (!player.hasEffect(ModEffects.ELECTRONIZE)) {
            return;
        }
        var motion = player.getDeltaMovement();
        player.setDeltaMovement(motion.x, Math.min(0.0, motion.y), motion.z);
        player.hurtMarked = true;
    }

    @SubscribeEvent
    public static void onProjectileImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof Projectile projectile)) {
            return;
        }
        if (!(event.getRayTraceResult() instanceof net.minecraft.world.phys.EntityHitResult hit)) {
            return;
        }
        if (hit.getEntity() instanceof net.minecraft.world.entity.player.Player player
                && player.hasEffect(ModEffects.ELECTRONIZE)) {
            event.setCanceled(true);
            projectile.setDeltaMovement(projectile.getDeltaMovement());
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ThunderstruckRuntime.clear(player);
        }
    }
}
