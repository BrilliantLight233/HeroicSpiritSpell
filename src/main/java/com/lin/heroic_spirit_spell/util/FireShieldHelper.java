package com.lin.heroic_spirit_spell.util;

import io.redspace.ironsspellbooks.entity.spells.AbstractShieldEntity;
import io.redspace.ironsspellbooks.entity.spells.ShieldPart;
import io.redspace.ironsspellbooks.entity.spells.shield.ShieldEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import com.lin.heroic_spirit_spell.network.FireShieldHudPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;

import javax.annotation.Nullable;
import java.util.UUID;
import java.util.Locale;

public final class FireShieldHelper {
    public static final String OBJECTIVE_HP = "fss_hp";
    public static final String OBJECTIVE_BLOCKED = "fss_blk";
    private static final String FIRE_SHIELD_MARKER_NAME = "heroic_spirit_spell_fire_shield";
    private static final String PLAYER_DATA = "heroic_spirit_spell_fire_shield";
    private static final String ACTIVE = "fire_active";
    private static final String OWNER_UUID = "fire_owner_uuid";
    private static final String PREV_HP = "prev_hp";
    private static final String HP_CENTI = "hp_centi";
    private static final String MAX_HP_CENTI = "max_hp_centi";
    private static final String BREAK_TICKS = "break_ticks";
    private static final String BLOCKED_CENTI = "blocked_centi";
    private static final String LAST_LEVEL = "last_level";

    public static final int FIRE_SHIELD_BREAK_DISABLED_TICKS = 100;

    private FireShieldHelper() {
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
        int value = Math.max(0, hpCenti);
        tag.putInt(HP_CENTI, value);
        savePlayerData(player, tag);
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.getServer() != null) {
            ensureHpObjective(serverPlayer.getServer());
            setHpObjectiveScore(serverPlayer, value);
        }
    }

    /**
     * Pull hp from scoreboard objective fss_hp if the player has an entry.
     * This allows /scoreboard edits to drive shield hp.
     */
    public static void applyScoreboardHpOverride(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        Objective objective = server.getScoreboard().getObjective(OBJECTIVE_HP);
        if (objective == null) {
            return;
        }
        ReadOnlyScoreInfo info = server.getScoreboard().getPlayerScoreInfo(player, objective);
        if (info == null) {
            return;
        }
        int maxHp = Math.max(0, getStoredMaxHpCenti(player));
        int clamped = Mth.clamp(info.value(), 0, maxHp);
        if (clamped != getStoredHpCenti(player)) {
            setStoredHpCenti(player, clamped);
        }
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

    public static int getLastSpellLevel(Player player) {
        return getPlayerData(player).getInt(LAST_LEVEL);
    }

    public static int getBlockedDamageCenti(Player player) {
        return getPlayerData(player).getInt(BLOCKED_CENTI);
    }

    /**
     * Adds blocked damage to NBT and fss_blk.
     * Base value prefers scoreboard if player already has an entry.
     */
    public static void addBlockedDamageCenti(Player player, int blockedCenti) {
        if (blockedCenti <= 0) {
            return;
        }
        CompoundTag tag = getPlayerData(player);
        int base = tag.getInt(BLOCKED_CENTI);
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.getServer() != null) {
            MinecraftServer server = serverPlayer.getServer();
            ensureBlockedObjective(server);
            Objective objective = server.getScoreboard().getObjective(OBJECTIVE_BLOCKED);
            if (objective != null) {
                ReadOnlyScoreInfo info = server.getScoreboard().getPlayerScoreInfo(serverPlayer, objective);
                if (info != null) {
                    base = info.value();
                }
            }
        }
        int newTotal = base + blockedCenti;
        tag.putInt(BLOCKED_CENTI, newTotal);
        savePlayerData(player, tag);
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.getServer() != null) {
            setBlockedObjectiveScore(serverPlayer, newTotal);
        }
    }

    private static void ensureBlockedObjective(MinecraftServer server) {
        Objective objective = server.getScoreboard().getObjective(OBJECTIVE_BLOCKED);
        if (objective == null) {
            runCommand(server, "scoreboard objectives add " + OBJECTIVE_BLOCKED + " dummy");
        }
    }

    private static void ensureHpObjective(MinecraftServer server) {
        Objective objective = server.getScoreboard().getObjective(OBJECTIVE_HP);
        if (objective == null) {
            runCommand(server, "scoreboard objectives add " + OBJECTIVE_HP + " dummy");
        }
    }

    private static void setBlockedObjectiveScore(ServerPlayer player, int value) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        Objective objective = server.getScoreboard().getObjective(OBJECTIVE_BLOCKED);
        if (objective == null) {
            return;
        }
        server.getScoreboard().getOrCreatePlayerScore(player, objective).set(value);
    }

    private static void setHpObjectiveScore(ServerPlayer player, int value) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            return;
        }
        Objective objective = server.getScoreboard().getObjective(OBJECTIVE_HP);
        if (objective == null) {
            return;
        }
        server.getScoreboard().getOrCreatePlayerScore(player, objective).set(value);
    }

    private static void runCommand(MinecraftServer server, String command) {
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source, command);
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
        // Recover 1% max hp every 2 ticks while shield is inactive.
        if ((player.level().getGameTime() & 1L) != 0L) {
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
        if (lastLevel != spellLevel) {
            setStoredHpCenti(player, maxHp);
        } else {
            setStoredHpCenti(player, Math.min(getStoredHpCenti(player), maxHp));
        }
        setLastSpellLevel(player, spellLevel);
    }

    public static void initializeFireShield(ShieldEntity shieldEntity, ServerPlayer owner, int spellLevel) {
        int storedHpCenti = getStoredHpCenti(owner);
        CompoundTag data = shieldEntity.getPersistentData();
        data.putBoolean(ACTIVE, true);
        data.putUUID(OWNER_UUID, owner.getUUID());
        data.putInt(LAST_LEVEL, spellLevel);
        shieldEntity.setHealth(fromCenti(storedHpCenti));
        shieldEntity.setInvisible(true);
        shieldEntity.setCustomName(Component.literal(FIRE_SHIELD_MARKER_NAME));
        shieldEntity.setCustomNameVisible(false);
        data.putFloat(PREV_HP, shieldEntity.getHealth());
    }

    public static void refreshExistingShield(ShieldEntity shieldEntity, ServerPlayer owner, int spellLevel) {
        int maxHpCenti = getStoredMaxHpCenti(owner);
        int storedHpCenti = Mth.clamp(getStoredHpCenti(owner), 0, maxHpCenti);
        CompoundTag data = shieldEntity.getPersistentData();
        int previousLevel = data.getInt(LAST_LEVEL);
        data.putBoolean(ACTIVE, true);
        data.putUUID(OWNER_UUID, owner.getUUID());
        data.putInt(LAST_LEVEL, spellLevel);
        if (previousLevel != spellLevel) {
            shieldEntity.setHealth(fromCenti(storedHpCenti));
        } else if (shieldEntity.getHealth() > fromCenti(maxHpCenti)) {
            shieldEntity.setHealth(fromCenti(maxHpCenti));
        }
        shieldEntity.setInvisible(true);
        shieldEntity.setCustomName(Component.literal(FIRE_SHIELD_MARKER_NAME));
        shieldEntity.setCustomNameVisible(false);
        data.putFloat(PREV_HP, shieldEntity.getHealth());
    }

    public static boolean isFireShield(Entity entity) {
        if (entity instanceof ShieldPart shieldPart) {
            return isFireShield(shieldPart.parentEntity);
        }
        if (!(entity instanceof AbstractShieldEntity abstractShieldEntity)) {
            return false;
        }
        if (abstractShieldEntity.getPersistentData().getBoolean(ACTIVE)) {
            return true;
        }
        return abstractShieldEntity.hasCustomName()
                && abstractShieldEntity.getCustomName() != null
                && FIRE_SHIELD_MARKER_NAME.equals(abstractShieldEntity.getCustomName().getString());
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

    public static void updateShieldAnchor(ShieldEntity shieldEntity, LivingEntity owner) {
        var look = owner.getLookAngle().normalize();
        var anchor = owner.getEyePosition().add(look.scale(1.0));
        shieldEntity.setPos(anchor.x, anchor.y - 0.35, anchor.z);
        shieldEntity.setRotation(owner.getXRot(), owner.getYRot());
        shieldEntity.setInvisible(true);
    }

    @Nullable
    public static ShieldEntity findOwnedShield(Player owner) {
        return owner.level().getEntitiesOfClass(ShieldEntity.class, owner.getBoundingBox().inflate(64),
                        entity -> isFireShield(entity) && owner.getUUID().equals(getShieldOwnerUuid(entity)))
                .stream()
                .findFirst()
                .orElse(null);
    }

    public static ShieldEntity getOrCreateShield(ServerPlayer player, int spellLevel) {
        ShieldEntity shieldEntity = findOwnedShield(player);
        if (shieldEntity == null || !shieldEntity.isAlive()) {
            shieldEntity = new ShieldEntity(player.level(), fromCenti(getStoredHpCenti(player)));
            initializeFireShield(shieldEntity, player, spellLevel);
            updateShieldAnchor(shieldEntity, player);
            player.level().addFreshEntity(shieldEntity);
        } else {
            refreshExistingShield(shieldEntity, player, spellLevel);
        }
        return shieldEntity;
    }

    public static void syncShieldHealth(ServerPlayer player, ShieldEntity shieldEntity) {
        int currentHp = Math.min(getStoredMaxHpCenti(player), toCenti(Math.max(0.0f, shieldEntity.getHealth())));
        setStoredHpCenti(player, currentHp);
        shieldEntity.setInvisible(true);
        shieldEntity.getPersistentData().putFloat(PREV_HP, shieldEntity.getHealth());
    }

    /** Apply stored hp to active shield entity (used after scoreboard overrides). */
    public static void applyStoredHpToShieldEntity(ServerPlayer player, ShieldEntity shieldEntity) {
        int maxHpCenti = Math.max(0, getStoredMaxHpCenti(player));
        int hpCenti = Mth.clamp(getStoredHpCenti(player), 0, maxHpCenti);
        float hp = fromCenti(hpCenti);
        if (Math.abs(shieldEntity.getHealth() - hp) > 0.0001f) {
            shieldEntity.setHealth(hp);
        }
        shieldEntity.setInvisible(true);
        shieldEntity.getPersistentData().putFloat(PREV_HP, shieldEntity.getHealth());
    }

    public static boolean shouldIgnoreDamage(AbstractShieldEntity shieldEntity, @Nullable Entity attacker) {
        return HolyShieldHelper.shouldIgnoreDamage(shieldEntity, attacker);
    }

    public static void afterShieldDamaged(AbstractShieldEntity shieldEntity) {
        ServerPlayer owner = getShieldOwner(shieldEntity);
        if (owner == null) {
            return;
        }
        float previousHp = shieldEntity.getPersistentData().getFloat(PREV_HP);
        float currentHp = Math.max(0.0f, shieldEntity.getHealth());
        int blocked = Math.max(0, toCenti(previousHp) - toCenti(currentHp));
        if (blocked > 0) {
            addBlockedDamageCenti(owner, blocked);
        }
        setStoredHpCenti(owner, Math.min(getStoredMaxHpCenti(owner), toCenti(currentHp)));
        shieldEntity.getPersistentData().putFloat(PREV_HP, currentHp);
        if (currentHp <= 0.0f) {
            setBreakTicks(owner, FIRE_SHIELD_BREAK_DISABLED_TICKS);
            setStoredHpCenti(owner, 0);
            applyVanillaShieldItemCooldown(owner, FIRE_SHIELD_BREAK_DISABLED_TICKS);
            owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
            if (HolyShieldHelper.isActivelyUsingShield(owner)) {
                owner.releaseUsingItem();
            }
        }
    }

    private static void applyVanillaShieldItemCooldown(ServerPlayer owner, int ticks) {
        ItemCooldowns cooldowns = owner.getCooldowns();
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = owner.getItemInHand(hand);
            if (HolyShieldHelper.isShieldItem(stack)) {
                cooldowns.addCooldown(stack.getItem(), ticks);
            }
        }
    }

    public static void clearShield(ServerPlayer player) {
        ShieldEntity shieldEntity = findOwnedShield(player);
        if (shieldEntity != null) {
            shieldEntity.discard();
        }
    }

    public static boolean isFireShieldBarrierActive(ServerPlayer player) {
        if (getBreakTicks(player) > 0) {
            return false;
        }
        if (!HolyShieldHelper.hasShieldItem(player)) {
            return false;
        }
        if (!HolyShieldHelper.isActivelyUsingShield(player)) {
            return false;
        }
        return FireShieldRuntime.getInscribedSpellLevel(player) > 0;
    }

    public static Component buildHudLine(int hpCenti, int maxHpCenti, int breakTicks) {
        int maxHp = Math.max(1, maxHpCenti);
        int hp = Mth.clamp(hpCenti, 0, maxHp);
        int filled = Math.round((hp / (float) maxHp) * 20.0f);
        String bar = "\u25A0".repeat(Math.max(0, filled)) + "\u25A1".repeat(Math.max(0, 20 - filled));
        var line = Component.literal(bar + " " + fromCenti(hp) + "/" + fromCenti(maxHp));
        if (breakTicks > 0) {
            float sec = breakTicks / 20.0f;
            line.append(Component.literal(" "))
                    .append(Component.translatable("spell.heroic_spirit_spell.fire_shield.subtitle_break",
                            String.format(Locale.ROOT, "%.1f", sec)));
        }
        return line;
    }

    public static void syncFireShieldHudToClient(ServerPlayer player, boolean show) {
        if (show) {
            PacketDistributor.sendToPlayer(player, new FireShieldHudPayload(
                    true, getStoredHpCenti(player), getStoredMaxHpCenti(player), getBreakTicks(player)));
        } else {
            PacketDistributor.sendToPlayer(player, new FireShieldHudPayload(false, 0, 0, 0));
        }
    }
}
