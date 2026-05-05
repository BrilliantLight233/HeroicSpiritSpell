package com.lin.heroic_spirit_spell.util;

import com.lin.heroic_spirit_spell.HeroicSpiritSpell;
import com.lin.heroic_spirit_spell.mixin.accessor.ProjectileOwnerAccessor;
import com.lin.heroic_spirit_spell.network.HolyShieldHudPayload;
import io.redspace.ironsspellbooks.entity.spells.AbstractShieldEntity;
import io.redspace.ironsspellbooks.entity.spells.ShieldPart;
import io.redspace.ironsspellbooks.entity.spells.shield.ShieldEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.Locale;
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

    /** 魔法盾破碎后：法术禁用与原版物品冷却共用时长（5s） */
    public static final int HOLY_SHIELD_BREAK_DISABLED_TICKS = 100;

    private HolyShieldHelper() {
    }

    public static boolean hasShieldItem(LivingEntity livingEntity) {
        return isShieldItem(livingEntity.getMainHandItem()) || isShieldItem(livingEntity.getOffhandItem());
    }

    public static boolean isShieldItem(ItemStack stack) {
        return !stack.isEmpty() && stack.canPerformAction(ItemAbilities.SHIELD_BLOCK);
    }

    public static boolean isActivelyUsingShield(LivingEntity livingEntity) {
        return livingEntity.isUsingItem() && isShieldItem(livingEntity.getUseItem());
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
        int value = Math.max(0, hpCenti);
        CompoundTag tag = getPlayerData(player);
        tag.putInt(HP_CENTI, value);
        savePlayerData(player, tag);
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.getServer() != null) {
            ensureObjective(serverPlayer.getServer(), OBJECTIVE_HP);
            setHpObjectiveScore(serverPlayer, value);
        }
    }

    /**
     * Pull hp from scoreboard objective hss_hp if the player has an entry.
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

    public static int getBlockedDamageCenti(Player player) {
        return getPlayerData(player).getInt(BLOCKED_CENTI);
    }

    /**
     * Adds blocked damage to NBT and hss_blk. Base value is the scoreboard if the player has an entry
     * (so /scoreboard edits stick); otherwise NBT (persistence when no score line yet).
     */
    public static void addBlockedDamageCenti(Player player, int blockedCenti) {
        if (blockedCenti <= 0) {
            return;
        }
        CompoundTag tag = getPlayerData(player);
        int base = tag.getInt(BLOCKED_CENTI);
        if (player instanceof ServerPlayer serverPlayer && serverPlayer.getServer() != null) {
            MinecraftServer server = serverPlayer.getServer();
            ensureObjectives(server);
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

    /** Writes hss_blk only when blocked damage changes (not every tick). */
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

    /** Keeps hss_hp aligned with NBT whenever HP changes (e.g. after combat). Avoids applyScoreboardHpOverride restoring stale scores next tick. */
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

    /** 仅等级变化时拉满；盾碎后 hp=0 仍走 tickRecovery，举盾时不再因 hp<=0 瞬间回满 */
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

    public static void refreshExistingShield(ShieldEntity shieldEntity, ServerPlayer owner, int spellLevel) {
        int maxHpCenti = getStoredMaxHpCenti(owner);
        int storedHpCenti = Mth.clamp(getStoredHpCenti(owner), 0, maxHpCenti);
        CompoundTag data = shieldEntity.getPersistentData();
        int previousLevel = data.getInt(LAST_LEVEL);
        data.putBoolean(ACTIVE, true);
        data.putUUID(OWNER_UUID, owner.getUUID());
        data.putFloat(MAX_HP, fromCenti(maxHpCenti));
        data.putInt(LAST_LEVEL, spellLevel);
        if (previousLevel != spellLevel) {
            shieldEntity.setHealth(fromCenti(storedHpCenti));
        } else if (shieldEntity.getHealth() > fromCenti(maxHpCenti)) {
            shieldEntity.setHealth(fromCenti(maxHpCenti));
        }
        data.putFloat(PREV_HP, shieldEntity.getHealth());
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
        } else {
            refreshExistingShield(shieldEntity, player, spellLevel);
        }
        return shieldEntity;
    }

    public static void syncShieldHealth(ServerPlayer player, ShieldEntity shieldEntity) {
        int currentHp = Math.min(getStoredMaxHpCenti(player), toCenti(Math.max(0.0f, shieldEntity.getHealth())));
        setStoredHpCenti(player, currentHp);
        shieldEntity.getPersistentData().putFloat(MAX_HP, fromCenti(getStoredMaxHpCenti(player)));
        setEntityPreviousHp(shieldEntity, shieldEntity.getHealth());
    }

    /** Apply stored hp to active shield entity (used after scoreboard overrides). */
    public static void applyStoredHpToShieldEntity(ServerPlayer player, ShieldEntity shieldEntity) {
        int maxHpCenti = Math.max(0, getStoredMaxHpCenti(player));
        int hpCenti = Mth.clamp(getStoredHpCenti(player), 0, maxHpCenti);
        float hp = fromCenti(hpCenti);
        if (Math.abs(shieldEntity.getHealth() - hp) > 0.0001f) {
            shieldEntity.setHealth(hp);
        }
        shieldEntity.getPersistentData().putFloat(MAX_HP, fromCenti(maxHpCenti));
        setEntityPreviousHp(shieldEntity, shieldEntity.getHealth());
    }

    /**
     * Resolves who is actually hitting the shield. Some damage sources incorrectly set
     * {@link DamageSource#getDirectEntity()} to the shield owner while the real aggressor is {@link DamageSource#getEntity()}.
     */
    @Nullable
    public static Entity pickDamageAttacker(DamageSource source, @Nullable ServerPlayer shieldOwner) {
        Entity direct = source.getDirectEntity();
        Entity root = source.getEntity();
        if (shieldOwner != null && direct != null && direct.is(shieldOwner) && root != null && !root.is(shieldOwner)) {
            return root;
        }
        return direct != null ? direct : root;
    }

    public static boolean shouldIgnoreDamage(AbstractShieldEntity shieldEntity, @Nullable Entity attacker) {
        ServerPlayer owner = getShieldOwner(shieldEntity);
        if (owner == null || attacker == null) {
            return false;
        }
        Entity alliedSource = resolveProjectileShooter(attacker, shieldEntity.level());
        // Own / allied projectiles: skip. "Self" as a non-projectile is usually a mis-tagged source — still apply damage.
        if (alliedSource == owner && !(attacker instanceof Projectile)) {
            return false;
        }
        return alliedSource == owner || alliedSource.isAlliedTo(owner) || owner.isAlliedTo(alliedSource);
    }

    /** Spell projectiles may not have getOwner() cached yet; fall back to owner UUID on the server. */
    public static Entity resolveProjectileShooter(Entity attacker, Level level) {
        if (!(attacker instanceof Projectile projectile)) {
            return attacker;
        }
        Entity shooter = projectile.getOwner();
        if (shooter != null) {
            return shooter;
        }
        UUID uuid = ((ProjectileOwnerAccessor) projectile).getTrackedOwnerUuid();
        if (uuid != null && level instanceof ServerLevel serverLevel) {
            Entity resolved = serverLevel.getEntity(uuid);
            if (resolved != null) {
                return resolved;
            }
        }
        return attacker;
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
            setBreakTicks(owner, HOLY_SHIELD_BREAK_DISABLED_TICKS);
            setStoredHpCenti(owner, 0);
            applyVanillaShieldItemCooldown(owner, HOLY_SHIELD_BREAK_DISABLED_TICKS);
            owner.level().playSound(null, owner.getX(), owner.getY(), owner.getZ(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.0f, 1.0f);
            if (isActivelyUsingShield(owner)) {
                owner.releaseUsingItem();
            }
        }
    }

    /** 主副手盾牌物品走原版冷却条，与破碎禁用时间一致 */
    private static void applyVanillaShieldItemCooldown(ServerPlayer owner, int ticks) {
        ItemCooldowns cooldowns = owner.getCooldowns();
        for (InteractionHand hand : InteractionHand.values()) {
            ItemStack stack = owner.getItemInHand(hand);
            if (isShieldItem(stack)) {
                cooldowns.addCooldown(stack.getItem(), ticks);
            }
        }
    }

    public static void clearShield(ServerPlayer player) {
        ShieldEntity shieldEntity = findOwnedShield(player.level(), player);
        if (shieldEntity != null) {
            shieldEntity.discard();
        }
    }

    public static Component buildHudLine(int hpCenti, int maxHpCenti, int breakTicks) {
        int maxHp = Math.max(1, maxHpCenti);
        int hp = Mth.clamp(hpCenti, 0, maxHp);
        int filled = Math.round((hp / (float) maxHp) * 20.0f);
        String bar = "■".repeat(Math.max(0, filled)) + "□".repeat(Math.max(0, 20 - filled));
        var line = Component.literal(bar + " " + fromCenti(hp) + "/" + fromCenti(maxHp));
        if (breakTicks > 0) {
            float sec = breakTicks / 20.0f;
            line.append(Component.literal(" "))
                    .append(Component.translatable("spell.heroic_spirit_spell.holy_shield.subtitle_break",
                            String.format(Locale.ROOT, "%.1f", sec)));
        }
        return line;
    }

    public static Component getShieldHud(ServerPlayer player) {
        return buildHudLine(getStoredHpCenti(player), getStoredMaxHpCenti(player), getBreakTicks(player));
    }

    /** 自定义客户端 HUD（action bar 上方），不占原版 title/subtitle。 */
    public static void syncHolyShieldHudToClient(ServerPlayer player, boolean show) {
        if (show) {
            PacketDistributor.sendToPlayer(player, new HolyShieldHudPayload(true,
                    getStoredHpCenti(player), getStoredMaxHpCenti(player), getBreakTicks(player)));
        } else {
            PacketDistributor.sendToPlayer(player, new HolyShieldHudPayload(false, 0, 0, 0));
        }
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
        // hss_blk: only updated in addBlockedDamageCenti so /scoreboard changes are not overwritten each tick
        runCommand(server, "scoreboard players set " + name + " " + OBJECTIVE_BREAK + " " + getBreakTicks(player));
    }

    private static void runCommand(MinecraftServer server, String command) {
        CommandSourceStack source = server.createCommandSourceStack().withSuppressedOutput();
        server.getCommands().performPrefixedCommand(source, command);
    }

    /** True while blocking with a shield and Holy Shield is learned + inscribed (spell wheel). */
    public static boolean isHolyShieldBarrierActive(ServerPlayer player) {
        if (getBreakTicks(player) > 0) {
            return false;
        }
        if (!hasShieldItem(player)) {
            return false;
        }
        if (!isActivelyUsingShield(player)) {
            return false;
        }
        return HolyShieldRuntime.getInscribedSpellLevel(player) > 0;
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
