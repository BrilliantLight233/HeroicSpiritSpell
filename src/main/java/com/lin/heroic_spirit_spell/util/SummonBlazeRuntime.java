package com.lin.heroic_spirit_spell.util;

import io.redspace.ironsspellbooks.entity.spells.fireball.SmallMagicFireball;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks summon-blaze packs per player, positions, expiry, and flaming-barrage follow-up volley.
 */
public final class SummonBlazeRuntime {

    public static final int BLAZE_COUNT = 5;
    private static final int LIFETIME_TICKS = 20 * 15;
    private static final float FIREBALL_DAMAGE = 5.0f;
    private static final float BOOM_BARRAGE_SUPPORT_DAMAGE = 15.0f;

    private static final ConcurrentHashMap<java.util.UUID, ActiveSummon> BY_PLAYER = new ConcurrentHashMap<>();

    private SummonBlazeRuntime() {
    }

    static final class ActiveSummon {
        final int[] blazeEntityIds = new int[BLAZE_COUNT];
        long endGameTime;
        PendingBarrage pending;
    }

    static final class PendingBarrage {
        /** Wait 1 tick after barrage cast, then fire one per tick (see tickBarrage). */
        int delayTicks = 1;
        int shotsRemaining = 5;
        final int[] order = new int[BLAZE_COUNT];
    }

    public static boolean hasActive(ServerPlayer player) {
        return BY_PLAYER.containsKey(player.getUUID());
    }

    public static boolean isOwnedSummonBlaze(ServerPlayer owner, Blaze blaze) {
        ActiveSummon s = BY_PLAYER.get(owner.getUUID());
        if (s == null) {
            return false;
        }
        int id = blaze.getId();
        for (int i = 0; i < BLAZE_COUNT; i++) {
            if (s.blazeEntityIds[i] == id) {
                return true;
            }
        }
        return false;
    }

    public static void replace(ServerPlayer player, Blaze[] blazes) {
        clear(player);
        ActiveSummon s = new ActiveSummon();
        s.endGameTime = player.level().getGameTime() + LIFETIME_TICKS;
        for (int i = 0; i < BLAZE_COUNT; i++) {
            s.blazeEntityIds[i] = blazes[i] == null ? 0 : blazes[i].getId();
        }
        BY_PLAYER.put(player.getUUID(), s);
    }

    public static void clear(ServerPlayer player) {
        ActiveSummon removed = BY_PLAYER.remove(player.getUUID());
        if (removed == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        for (int i = 0; i < BLAZE_COUNT; i++) {
            Entity e = level.getEntity(removed.blazeEntityIds[i]);
            if (e != null) {
                e.discard();
            }
        }
    }

    public static void tickFollowAndExpiry(ServerPlayer player) {
        ActiveSummon s = BY_PLAYER.get(player.getUUID());
        if (s == null) {
            return;
        }
        if (player.level().getGameTime() >= s.endGameTime) {
            clear(player);
            return;
        }
        for (int i = 0; i < BLAZE_COUNT; i++) {
            Entity e = player.level().getEntity(s.blazeEntityIds[i]);
            if (!(e instanceof Blaze blaze) || !blaze.isAlive()) {
                continue;
            }
            Vec3 pos = slotPosition(player, i);
            blaze.setPos(pos.x, pos.y, pos.z);
            blaze.setYRot(player.getYRot());
            blaze.setYHeadRot(player.getYRot());
            blaze.setXRot(0f);
            blaze.setDeltaMovement(Vec3.ZERO);
        }
    }

    public static void onFlamingBarrageCast(ServerPlayer player) {
        ActiveSummon s = BY_PLAYER.get(player.getUUID());
        if (s == null || s.pending != null) {
            return;
        }
        PendingBarrage p = new PendingBarrage();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < BLAZE_COUNT; i++) {
            slots.add(i);
        }
        Collections.shuffle(slots);
        for (int i = 0; i < BLAZE_COUNT; i++) {
            p.order[i] = slots.get(i);
        }
        s.pending = p;
    }

    public static void onBoomBarrageCast(ServerPlayer player) {
        ActiveSummon s = BY_PLAYER.get(player.getUUID());
        if (s == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        Vec3 look = player.getViewVector(1.0f);
        for (int i = 0; i < BLAZE_COUNT; i++) {
            Entity e = level.getEntity(s.blazeEntityIds[i]);
            if (!(e instanceof Blaze blaze) || !blaze.isAlive()) {
                continue;
            }
            Vec3 origin = blaze.getEyePosition().subtract(0, 0.25, 0);
            BoomBarrageRuntime.spawnTriple(
                    level,
                    player,
                    origin,
                    look,
                    BOOM_BARRAGE_SUPPORT_DAMAGE,
                    BoomBarrageRuntime.EXPLOSION_RADIUS);
        }
    }

    public static void tickBarrage(ServerPlayer player) {
        ActiveSummon s = BY_PLAYER.get(player.getUUID());
        if (s == null || s.pending == null) {
            return;
        }
        PendingBarrage p = s.pending;
        if (p.delayTicks > 0) {
            p.delayTicks--;
            if (p.delayTicks > 0) {
                return;
            }
        }
        if (p.shotsRemaining <= 0) {
            s.pending = null;
            return;
        }
        int idx = BLAZE_COUNT - p.shotsRemaining;
        int slot = p.order[idx];
        spawnFireballFromBlaze(player, s, slot);
        p.shotsRemaining--;
        if (p.shotsRemaining <= 0) {
            s.pending = null;
        }
    }

    private static void spawnFireballFromBlaze(ServerPlayer player, ActiveSummon s, int slot) {
        ServerLevel level = player.serverLevel();
        Entity e = level.getEntity(s.blazeEntityIds[slot]);
        if (!(e instanceof Blaze blaze) || !blaze.isAlive()) {
            return;
        }
        Vec3 eye = blaze.getEyePosition();
        SmallMagicFireball fb = new SmallMagicFireball(level, player);
        fb.setPos(eye.x, eye.y - 0.25, eye.z);
        fb.setDamage(FIREBALL_DAMAGE);
        fb.setCursorHoming(true);
        Vec3 look = player.getViewVector(1.0f);
        fb.shoot(look.scale(0.5f), 0.0f);
        level.addFreshEntity(fb);
    }

    /** Feet-relative offsets: top+2.5; R2+1; L2+1; R1.5+2; L1.5+2 */
    public static Vec3 slotPosition(ServerPlayer player, int slot) {
        Vec3 feet = player.position();
        Vec3 right = new Vec3(-player.getForward().z, 0.0, player.getForward().x).normalize();
        Vec3 headTop = feet.add(0.0, player.getBbHeight(), 0.0);
        return switch (slot) {
            case 0 -> headTop.add(0.0, 2.5, 0.0);
            case 1 -> feet.add(right.scale(2.0)).add(0.0, 1.0, 0.0);
            case 2 -> feet.add(right.scale(-2.0)).add(0.0, 1.0, 0.0);
            case 3 -> feet.add(right.scale(1.5)).add(0.0, 2.0, 0.0);
            case 4 -> feet.add(right.scale(-1.5)).add(0.0, 2.0, 0.0);
            default -> feet;
        };
    }
}
