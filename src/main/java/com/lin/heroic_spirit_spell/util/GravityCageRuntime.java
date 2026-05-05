package com.lin.heroic_spirit_spell.util;

import com.lin.heroic_spirit_spell.network.GravityCageTrapPayload;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-only: tracks cages keyed by caster UUID (spell is player-cast).
 */
public final class GravityCageRuntime {

    public static final float RADIUS = 5.0f;
    private static final double R = RADIUS;
    private static final double BOUNDARY_PUSH_INNER = R - 0.62;
    private static final double ENTITY_BAND_MIN = R - 0.48;
    private static final double ENTITY_BAND_MAX = R + 0.02;
    private static final double VERTICAL_REACH = 5.0;
    /** Every 5 ticks; ~25% of former 10/tick -> 2-3 per burst */
    private static final int PARTICLE_INTERVAL_TICKS = 5;
    private static final int PARTICLES_PER_BURST = 3;

    public record Cage(ResourceKey<Level> dimension, int endGameTime, int visualEntityId) {
    }

    private static final Map<UUID, Cage> BY_CASTER = new ConcurrentHashMap<>();
    private static final Set<UUID> PREV_TRAPPED_PLAYERS = ConcurrentHashMap.newKeySet();

    private GravityCageRuntime() {
    }

    public static boolean hasActiveCage(UUID casterUuid) {
        return BY_CASTER.containsKey(casterUuid);
    }

    /** True if this caster has a cage entry in this dimension (avoids stale keys from other dimensions). */
    public static boolean hasCageActiveHere(ServerLevel sl, UUID casterUuid) {
        Cage c = BY_CASTER.get(casterUuid);
        return c != null && c.dimension().equals(sl.dimension());
    }

    /**
     * Horizontal radius {@link #R} from caster feet; vertical band matches cage. Used for collapse pull and trapped-player HUD sync.
     */
    private static boolean isNonAllyWithinCasterCageCylinder(LivingEntity caster, LivingEntity other) {
        if (other == caster || other.isAlliedTo(caster)) {
            return false;
        }
        Vec3 feet = caster.position();
        double r2 = R * R;
        double yLow = feet.y - 2.0;
        double yHigh = feet.y + VERTICAL_REACH + 3.0;
        AABB bb = other.getBoundingBox();
        if (bb.maxY < yLow || bb.minY > yHigh) {
            return false;
        }
        double cx = Mth.clamp(feet.x, bb.minX, bb.maxX);
        double cz = Mth.clamp(feet.z, bb.minZ, bb.maxZ);
        double dx = feet.x - cx;
        double dz = feet.z - cz;
        return dx * dx + dz * dz <= r2 + 1e-4;
    }

    private static AABB cylinderBroadPhaseBox(Vec3 feet) {
        double yLow = feet.y - 2.0;
        double yHigh = feet.y + VERTICAL_REACH + 3.0;
        return new AABB(feet.x - R - 1.0, yLow, feet.z - R - 1.0, feet.x + R + 1.0, yHigh, feet.z + R + 1.0);
    }

    /**
     * Collapse group pull while cage is active (same cylinder as subtitle / cage logic).
     */
    public static List<LivingEntity> listNonAlliesNearCasterWhenCageActiveForCollapse(ServerLevel sl, LivingEntity caster) {
        Cage cage = BY_CASTER.get(caster.getUUID());
        if (cage == null || !cage.dimension().equals(sl.dimension())) {
            return List.of();
        }
        Vec3 feet = caster.position();
        AABB box = cylinderBroadPhaseBox(feet);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity le : sl.getEntitiesOfClass(LivingEntity.class, box)) {
            if (isNonAllyWithinCasterCageCylinder(caster, le)) {
                out.add(le);
            }
        }
        return out;
    }

    public static void start(ServerLevel level, LivingEntity caster, int visualEntityId, int durationTicks) {
        UUID id = caster.getUUID();
        Cage prev = BY_CASTER.get(id);
        if (prev != null) {
            if (prev.dimension().equals(level.dimension())) {
                discardVisual(level, prev.visualEntityId());
            } else {
                ServerLevel other = level.getServer().getLevel(prev.dimension());
                if (other != null) {
                    discardVisual(other, prev.visualEntityId());
                }
            }
        }
        int end = level.getServer().getTickCount() + durationTicks;
        BY_CASTER.put(id, new Cage(level.dimension(), end, visualEntityId));
    }

    public static void clearCaster(UUID casterId) {
        Cage c = BY_CASTER.remove(casterId);
        if (c == null) {
            return;
        }
        var s = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (s == null) {
            return;
        }
        ServerLevel sl = s.getLevel(c.dimension());
        if (sl != null) {
            discardVisual(sl, c.visualEntityId());
        }
    }

    private static void discardVisual(ServerLevel sl, int entityId) {
        Entity e = sl.getEntity(entityId);
        if (e != null) {
            e.discard();
        }
    }

    public static void tick(ServerLevel level) {
        int now = level.getServer().getTickCount();
        ResourceKey<Level> dim = level.dimension();
        List<UUID> removeIds = new ArrayList<>();
        for (Map.Entry<UUID, Cage> e : BY_CASTER.entrySet()) {
            Cage cage = e.getValue();
            if (!cage.dimension().equals(dim)) {
                continue;
            }
            ServerPlayer caster = level.getServer().getPlayerList().getPlayer(e.getKey());
            boolean expired = caster == null || !caster.isAlive() || now >= cage.endGameTime();
            if (expired) {
                discardVisual(level, cage.visualEntityId());
                removeIds.add(e.getKey());
                continue;
            }
            Vec3 origin = caster.position();
            tickLivingPush(level, caster, origin);
            if (now % PARTICLE_INTERVAL_TICKS == 0) {
                tickParticlesBurst(level, origin);
            }
        }
        for (UUID id : removeIds) {
            BY_CASTER.remove(id);
        }
    }

    public static void syncTrappedPlayersGlobal(MinecraftServer server) {
        Set<UUID> curr = new HashSet<>();
        for (Map.Entry<UUID, Cage> e : BY_CASTER.entrySet()) {
            Cage cage = e.getValue();
            ServerLevel sl = server.getLevel(cage.dimension());
            if (sl == null) {
                continue;
            }
            ServerPlayer caster = server.getPlayerList().getPlayer(e.getKey());
            if (caster == null || !caster.isAlive()) {
                continue;
            }
            Vec3 feet = caster.position();
            AABB box = cylinderBroadPhaseBox(feet);
            for (ServerPlayer vic : sl.getEntitiesOfClass(ServerPlayer.class, box)) {
                if (isNonAllyWithinCasterCageCylinder(caster, vic)) {
                    curr.add(vic.getUUID());
                }
            }
        }
        // Resend true each tick: recover from lost packets; action bar only on enter/exit edge.
        Component trappedBar = Component.translatable("message.heroic_spirit_spell.gravity_cage_trapped")
                .withStyle(ChatFormatting.RED);
        for (UUID u : curr) {
            ServerPlayer p = server.getPlayerList().getPlayer(u);
            if (p != null) {
                PacketDistributor.sendToPlayer(p, new GravityCageTrapPayload(true));
                if (!PREV_TRAPPED_PLAYERS.contains(u)) {
                    p.connection.send(new ClientboundSetActionBarTextPacket(trappedBar));
                }
            }
        }
        for (UUID u : PREV_TRAPPED_PLAYERS) {
            if (!curr.contains(u)) {
                ServerPlayer p = server.getPlayerList().getPlayer(u);
                if (p != null) {
                    PacketDistributor.sendToPlayer(p, new GravityCageTrapPayload(false));
                    p.connection.send(new ClientboundSetActionBarTextPacket(Component.empty()));
                }
            }
        }
        PREV_TRAPPED_PLAYERS.clear();
        PREV_TRAPPED_PLAYERS.addAll(curr);
    }

    private static void tickLivingPush(ServerLevel level, LivingEntity caster, Vec3 origin) {
        AABB box = new AABB(origin, origin).inflate(R + 0.5, VERTICAL_REACH, R + 0.5);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (victim == caster || victim.isAlliedTo(caster)) {
                continue;
            }
            Vec3 ep = victim.getBoundingBox().getCenter();
            if (Math.abs(ep.y - origin.y) > VERTICAL_REACH) {
                continue;
            }
            double h = horizDist(ep, origin);
            if (h <= ENTITY_BAND_MIN || h > ENTITY_BAND_MAX) {
                continue;
            }
            Vec3 horiz = new Vec3(ep.x - origin.x, 0, ep.z - origin.z);
            double len = horiz.length();
            if (len < 1e-5) {
                continue;
            }
            Vec3 dir = horiz.scale(1.0 / len);
            double nx = origin.x + dir.x * BOUNDARY_PUSH_INNER;
            double nz = origin.z + dir.z * BOUNDARY_PUSH_INNER;
            double vy = victim.getY();
            if (victim instanceof ServerPlayer sp) {
                sp.teleportTo(level, nx, vy, nz, Set.of(), victim.getYRot(), victim.getXRot());
            } else {
                victim.moveTo(nx, vy, nz, victim.getYRot(), victim.getXRot());
            }
            victim.setDeltaMovement(victim.getDeltaMovement().multiply(0.25, 1.0, 0.25));
            victim.hurtMarked = true;
        }
    }

    private static void tickParticlesBurst(ServerLevel level, Vec3 origin) {
        var rnd = level.random;
        for (int i = 0; i < PARTICLES_PER_BURST; i++) {
            double ang = rnd.nextDouble() * Math.PI * 2;
            double rr = rnd.nextDouble() * (R - 0.35);
            double x = origin.x + Math.cos(ang) * rr;
            double z = origin.z + Math.sin(ang) * rr;
            double y = origin.y + rnd.nextDouble() * 2.2;
            MagicManager.spawnParticles(level, ParticleTypes.DRIPPING_OBSIDIAN_TEAR, x, y, z, 1, 0.02, 0.08, 0.02, 0.002, false);
        }
    }

    private static double horizDist(Vec3 p, Vec3 o) {
        double dx = p.x - o.x;
        double dz = p.z - o.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
