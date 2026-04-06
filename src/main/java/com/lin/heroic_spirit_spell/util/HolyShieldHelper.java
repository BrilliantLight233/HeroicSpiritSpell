package com.lin.heroic_spirit_spell.util;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.registry.ModSpells;
import io.redspace.ironsspellbooks.entity.spells.AbstractShieldEntity;
import io.redspace.ironsspellbooks.entity.spells.ShieldPart;
import io.redspace.ironsspellbooks.entity.spells.shield.ShieldEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Objective;
import net.neoforged.neoforge.common.ItemAbilities;

import javax.annotation.Nullable;
import java.util.UUID;

public final class HolyShieldHelper {
    public static final String OBJECTIVE_HP = "hss_hp";
    public static final String OBJECTIVE_MAX_HP = "hss_max";
    public static final String OBJECTIVE_BLOCKED = "hss_blk";
    public static final String OBJECTIVE_BREAK = "hss_brk";

    private static final String PLAYER_DATA = HeroicSpiritSpell.MODID + "_holy_shield";
    private static final String ACTIVE = "active";
    private static final String OWNER_UUID = "owner_uuid";
    private static final String MAX_HP = "max_hp";
    private static final String PREV_HP = "prev_hp";
    private static final String HP_CENTI = "hp_centi";
    private static final String MAX_HP_CENTI = "max_hp_centi";
    private static final String BREAK_TICKS = "break_ticks";
    private static final String BLOCKED_CENTI = "blocked_centi";
    private static final String LAST_LEVEL = "last_level";

    private HolyShieldHelper() {
    }

    public static boolean hasShieldItem(LivingEntity livingEntity) {
        return isShieldItem(livingEntity.getMainHandItem()) || isShieldItem(livingEntity.getOffhandItem());
    }

    public static boolean isShieldItem(ItemStack stack) {
        return !stack.isEmpty() && stack.canPerformAction(ItemAbilities.SHIELD_BLOCK);
    }

    public static int getMaxHpCenti(int spellLevel) {
        return spellLevel * 1000;
    }

    public static int toCenti(float health) {
        return Math.max(0, Mth.floor(health * 100.0f + 0.5f));
    }

    public static float fromCenti(int healthCenti) {
        return healthCenti / 100.0f;
    }

    public static CompoundTag getPlayerData(Player player) {
        return player.getPersistentData().getCompound(PLAYER_DATA);
    }

    public static void savePlayerData(Player player, CompoundTag tag) {
        player.getPersistentData().put(PLAYER_DATA, tag);
    }

    public static int getStoredHpCenti(Player player) {
        return getPlayerData(player).getInt(HP_CENTI);
    }

    public static void setStoredHpCenti(Player player, int hpCenti) {
        CompoundTag tag = getPlayerData(player);
        tag.putInt(HP_CENTI, Math.max(0, hpCenti));
        savePlayerData(player, tag);
    }

    public static int getStoredMaxHpCenti(Player player) {
        return getPlayerData(player).getInt(MAX_HP_CENTI);
    }

    public static void setStoredMaxHpCenti(Player player, int maxHpCenti) {
        CompoundTag tag = getPlayerData(player);
        tag.putInt(MAX_HP_CENTI, Math.max(0, maxHpCenti));
        savePlayerData(player, tag);
    }

    public static int getBreakTicks(Player player) {
        return getPlayerData(player).getInt(BREAK_TICKS);
    }

    public static void setBreakTicks(Player player, int breakTicks) {
        CompoundTag tag = getPlayerData(player);
        tag.putInt(BREAK_TICKS, Math.max(0, breakTicks));
        savePlayerData(player, tag);
    }

    public static int getBlockedDamageCenti(Player player) {
        return getPlayerData(player).getInt(BLOCKED_CENTI);
    }

    public static void addBlockedDamageCenti(Player player, int blockedCenti) {
        if (blockedCenti <= 0) {
            return;
        }
        CompoundTag tag = getPlayerData(player);
        tag.putInt(BLOCKED_CENTI, tag.getInt(BLOCKED_CENTI) + blockedCenti);
        savePlayerData(player, tag);
    }

    public static int getLastSpellLevel(Player player) {
        return getPlayerData(player).getInt(LAST_LEVEL);
    }

    public static void setLastSpellLevel(Player player, int spellLevel) {
        CompoundTag tag = getPlayerData(player);
        tag.putInt(LAST_LEVEL, spellLevel);
        savePlayerData(player, tag);
    }

    public static void tickRecovery(ServerPlayer player) {
        int breakTicks = getBreakTicks(player);
        if (breakTicks > 0) {
            setBreakTicks(player, breakTicks - 1);
            return;
        }
        int maxHp = getStoredMaxHpCenti(player);
        int hp = getStoredHpCenti(player);
        if (maxHp <= 0 || hp >= maxHp) {
            return;
        }
        setStoredHpCenti(player, Math.min(maxHp, hp + Math.max(1, maxHp / 100)));
    }

    public static void ensureCapacityForLevel(ServerPlayer player, int spellLevel) {
        int maxHp = getMaxHpCenti(spellLevel);
        int lastLevel = getLastSpellLevel(player);
        setStoredMaxHpCenti(player, maxHp);
        if (lastLevel != spellLevel || getStoredHpCenti(player) <= 0) {
            setStoredHpCenti(player, maxHp);
        } else {
            setStoredHpCenti(player, Math.min(getStoredHpCenti(player), maxHp));
        }
        setLastSpellLevel(player, spellLevel);
    }

    public static void initializeHolyShield(ShieldEntity shieldEntity, ServerPlayer owner, int spellLevel) {
        int maxHpCenti = getStoredMaxHpCenti(owner);
        int storedHpCenti = getStoredHpCenti(owner);
        shieldEntity.setHealth(fromCenti(storedHpCenti));
        CompoundTag data = shieldEntity.getPersistentData();
        data.putBoolean(ACTIVE, true);
        data.putUUID(OWNER_UUID, owner.getUUID());
        data.putFloat(MAX_HP, fromCenti(maxHpCenti));
        data.putFloat(PREV_HP, shieldEntity.getHealth());
        data.putInt(LAST_LEVEL, spellLevel);
        shieldEntity.setGlowingTag(true);
    }

    public static boolean isHolyShield(Entity entity) {
        if (entity instanceof ShieldPart shieldPart) {
            return isHolyShield(shieldPart.parentEntity);
        }
        return entity instanceof AbstractShieldEntity abstractShieldEntity && abstractShieldEntity.getPersistentData().getBoolean(ACTIVE);
    }

    @Nullable
    public static UUID getShieldOwnerUuid(Entity entity) {
        Entity target = entity instanceof ShieldPart shieldPart ? shieldPart.parentEntity : entity;
        if (!(target instanceof AbstractShieldEntity abstractShieldEntity)) {
            return null;
        }
        CompoundTag data = abstractShieldEntity.getPersistentData();
        return data.hasUUID(OWNER_UUID) ? data.getUUID(OWNER_UUID) : null;
    }

    @Nullable
    public static ServerPlayer getShieldOwner(Entity entity) {
        UUID uuid = getShieldOwnerUuid(entity);
        if (uuid == null || entity.level().getServer() == null) {
            return null;
        }
        return entity.level().getServer().getPlayerList().getPlayer(uuid);
    }

    public static float getEntityMaxHp(AbstractShieldEntity shieldEntity) {
        return shieldEntity.getPersistentData().getFloat(MAX_HP);
    }

    public static void setEntityPreviousHp(AbstractShieldEntity shieldEntity, float hp) {
        shieldEntity.getPersistentData().putFloat(PREV_HP, hp);
    }

    public static float getEntityPreviousHp(AbstractShieldEntity shieldEntity) {
        return shieldEntity.getPersistentData().getFloat(PREV_HP);
    }

    public static void updateShieldAnchor(ShieldEntity shieldEntity, LivingEntity owner) {
        var look = owner.getLookAngle().normalize();
        var anchor = owner.getEyePosition().add(look.scale(1.0));
        shieldEntity.setPos(anchor.x, anchor.y - 0.35, anchor.z);
        shieldEntity.setRotation(owner.getXRot(), owner.getYRot());
        shieldEntity.setGlowingTag(true);
    }

    @Nullable
    public static ShieldEntity findOwnedShield(Level level, Player owner) {
        return level.getEntitiesOfClass(ShieldEntity.class, owner.getBoundingBox().inflate(64),
                        entity -> isHolyShield(entity) && owner.getUUID().equals(getShieldOwnerUuid(entity)))
                .stream()
                .findFirst()
                .orElse(null);
    }

    public static ShieldEntity getOrCreateShield(ServerPlayer player, int spellLevel) {
        ShieldEntity shieldEntity = findOwnedShield(player.level(), player);
        if (shieldEntity == null || !shieldEntity.isAlive()) {
            shieldEntity = new ShieldEntity(player.level(), fromCenti(getStoredHpCenti(player)));
            initializeHolyShield(shieldEntity, player, spellLevel);
            updateShieldAnchor(shieldEntity, player);
            player.level().addFreshEntity(shieldEntity);
        }
        return shieldEntity;
    }

    public static void syncShieldHealth(ServerPlayer player, ShieldEntity shieldEntity) {
        int currentHp = Math.min(getStoredMaxHpCenti(player), toCenti(Math.max(0.0f, shieldEntity.getHealth())));
        setStoredHpCenti(player, currentHp);
        setEntityPreviousHp(shieldEntity, shieldEntity.getHealth());
    }

    public static boolean shouldIgnoreDamage(AbstractShieldEntity shieldEntity, @Nullable Entity attacker) {
        ServerPlayer owner = getShieldOwner(shieldEntity);
        if (owner == null || attacker == null) {
            return false;
        }
        Entity alliedSource = attacker;
        if (attacker instanceof net.minecraft.world.entity.projectile.Projectile projectile && projectile.getOwner() != null) {
            alliedSource = projectile.getOwner();
        }
        return alliedSource == owner || alliedSource.isAlliedTo(owner) || owner.isAlliedTo(alliedSource);
    }

    public static void afterShieldDamaged(AbstractShieldEntity shieldEntity) {
        ServerPlayer owner = getShieldOwner(shieldEntity);
        if (owner == null) {
            return;
        }
        float previousHp = getEntityPreviousHp(shieldEntity);
        float currentHp = Math.max(0.0f, shieldEntity.getHealth());
        int blocked = Math.max(0, toCenti(previousHp) - toCenti(currentHp));
        if (blocked > 0) {
            addBlockedDamageCenti(owner, blocked);
        }
        setStoredHpCenti(owner, Math.min(getStoredMaxHpCenti(owner), toCenti(currentHp)));
        setEntityPreviousHp(shieldEntity, currentHp);
        if (currentHp <= 0.0f) {
            setBreakTicks(owner, 100);
            setStoredHpCenti(owner, 0);
            owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
        }
    }

    public static void clearShield(ServerPlayer player) {
        ShieldEntity shieldEntity = findOwnedShield(player.level(), player);
        if (shieldEntity != null) {
            shieldEntity.discard();
        }
    }

    public static Component getShieldHud(ServerPlayer player) {
        int maxHp = Math.max(1, getStoredMaxHpCenti(player));
        int hp = Mth.clamp(getStoredHpCenti(player), 0, maxHp);
        int filled = Math.round((hp / (float) maxHp) * 20.0f);
        String bar = "■".repeat(Math.max(0, filled)) + "□".repeat(Math.max(0, 20 - filled));
        return Component.literal(bar + " " + fromCenti(hp) + "/" + fromCenti(maxHp));
    }

    public static void sendHud(ServerPlayer player) {
        player.connection.send(new ClientboundSetActionBarTextPacket(getShieldHud(player)));
    }

    public static ResourceLocationHolder getShieldTextures(AbstractShieldEntity shieldEntity) {
        float maxHp = Math.max(0.001f, getEntityMaxHp(shieldEntity));
        float ratio = Mth.clamp(shieldEntity.getHealth() / maxHp, 0.0f, 1.0f);
        if (ratio < 0.25f) {
            return new ResourceLocationHolder(
                    HeroicSpiritSpell.MODID,
                    "textures/entity/shield/shield_overlay_0_24.png",
                    "textures/entity/shield/shield_trim_0_24.png");
        }
        if (ratio < 0.5f) {
            return new ResourceLocationHolder(
                    HeroicSpiritSpell.MODID,
                    "textures/entity/shield/shield_overlay_25_49.png",
                    "textures/entity/shield/shield_trim_25_49.png");
        }
        return new ResourceLocationHolder(
                HeroicSpiritSpell.MODID,
                "textures/entity/shield/shield_overlay_50_100.png",
                "textures/entity/shield/shield_trim_50_100.png");
    }

    public static void ensureObjectives(MinecraftServer server) {
        ensureObjective(server, OBJECTIVE_HP);
        ensureObjective(server, OBJECTIVE_MAX_HP);
        ensureObjective(server, OBJECTIVE_BLOCKED);
        ensureObjective(server, OBJECTIVE_BREAK);
    }

    private static void ensureObjective(MinecraftServer server, String objectiveName) {
        Objective objective = server.getScoreboard().getObjective(objectiveName);
        if (objective == null) {
            runCommand(server, "scoreboard objectives add " + objectiveName + " dummy");
        }
    }

    public static void syncScoreboard(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        ensureObjectives(server);
        String name = player.getScoreboardName();
        runCommand(server, "scoreboard players set " + name + " " + OBJECTIVE_HP + " " + getStoredHpCenti(player));
        runCommand(server, "scoreboard players set " + name + " " + OBJECTIVE_MAX_HP + " " + getStoredMaxHpCenti(player));
        runCommand(server, "scoreboard players set " + name + " " + OBJECTIVE_BLOCKED + " " + getBlockedDamageCenti(player));
        runCommand(server, "scoreboard players set " + name + " " + OBJECTIVE_BREAK + " " + getBreakTicks(player));
    }

    private static void runCommand(MinecraftServer server, String command) {
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source, command);
    }

    public static boolean isHolyShieldSpellActive(ServerPlayer player) {
        var magicData = io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(player);
        return magicData.isCasting() && (HeroicSpiritSpell.MODID + ":holy_shield").equals(magicData.getCastingSpellId());
    }

    public record ResourceLocationHolder(String namespace, String overlayPath, String trimPath) {
        public net.minecraft.resources.ResourceLocation overlay() {
            return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(namespace, overlayPath);
        }

        public net.minecraft.resources.ResourceLocation trim() {
            return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(namespace, trimPath);
        }
    }
}
