package dev.galar.rank;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Physical, server-owned Rotom companion. Cobblemon renders its existing Rotom
 * model; this mod does not redistribute any Cobblemon model/texture assets.
 *
 * Experimental: this is a PokemonEntity, not yet a dedicated non-catchable drone
 * entity. Keep it out of competitive gameplay until interaction hooks are tested.
 */
public final class RotomCompanion {
    public static final String DRONE_TAG = "galarrank_drone";
    public static final String OWNER_TAG_PREFIX = "galarrank_drone_owner_";
    private static final Map<UUID, UUID> ACTIVE = new HashMap<>();

    public RotomCompanion() {}

    /** Returns true if summoned; false if the existing companion was recalled. */
    public static boolean toggle(ServerPlayer player) {
        UUID owner = player.getUUID();
        PokemonEntity current = find(player.getServer(), ACTIVE.get(owner));
        if (current != null && current.getTags().contains(DRONE_TAG)) {
            current.discard();
            ACTIVE.remove(owner);
            player.sendSystemMessage(Component.literal("§6[Rotom Drone] §7Recolhido!"));
            return false;
        }
        ACTIVE.remove(owner);
        try {
            // createEntity uses Cobblemon's own entity renderer/model and species data.
            PokemonEntity drone = PokemonProperties.Companion.parse("rotom level=5")
                    .createEntity(player.serverLevel());
            Vec3 start = player.position().add(0.8, 2.1, 0.8);
            drone.moveTo(start.x, start.y, start.z, 0f, 0f);
            drone.setNoGravity(true);
            drone.setNoAi(true);
            drone.setInvulnerable(true);
            drone.setSilent(true);
            drone.setCustomName(Component.literal("§6Rotom Drone"));
            drone.setCustomNameVisible(true);
            drone.addTag(DRONE_TAG);
            drone.addTag(OWNER_TAG_PREFIX + owner);
            if (!player.serverLevel().addFreshEntity(drone)) {
                player.sendSystemMessage(Component.literal("§c[Rotom Drone] Nao foi possivel invocar Rotom."));
                return false;
            }
            ACTIVE.put(owner, drone.getUUID());
            player.sendSystemMessage(Component.literal("§6[Rotom Drone] §aRotom chegou! Clique nele para conferir o radar. K para recolher."));
            return true;
        } catch (Exception ex) {
            GalarRank.LOGGER.error("[GalarRank] Falha ao criar Rotom fisico", ex);
            player.sendSystemMessage(Component.literal("§c[Rotom Drone] Erro ao invocar. Confira latest.log."));
            return false;
        }
    }

    public static boolean isOwner(ServerPlayer player, Entity drone) {
        return drone.getTags().contains(DRONE_TAG)
                && drone.getTags().contains(OWNER_TAG_PREFIX + player.getUUID());
    }

    private static PokemonEntity find(MinecraftServer server, UUID id) {
        if (id == null || server == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(id);
            if (entity instanceof PokemonEntity pokemon && pokemon.isAlive()) return pokemon;
        }
        return null;
    }

    @SubscribeEvent
    public void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        // Update at 10Hz to reduce server load, without needing a custom renderer.
        if (server.getTickCount() % 2 != 0) return;
        for (var entry : new HashMap<>(ACTIVE).entrySet()) {
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
            PokemonEntity drone = find(server, entry.getValue());
            if (owner == null || drone == null) {
                if (drone != null) drone.discard();
                ACTIVE.remove(entry.getKey());
                continue;
            }
            if (drone.level() != owner.level()) {
                drone.discard();
                ACTIVE.remove(entry.getKey());
                continue;
            }
            Vec3 target = owner.position().add(0.9, 1.75 + 0.12 * Math.sin(server.getTickCount() / 9.0), 0.8);
            Vec3 displacement = target.subtract(drone.position());
            Vec3 next = displacement.lengthSqr() > 256 ? target : drone.position().add(displacement.scale(0.30));
            drone.teleportTo(next.x, next.y, next.z);
            drone.setDeltaMovement(Vec3.ZERO);
            drone.setNoGravity(true);
        }
    }

    public static void clear(MinecraftServer server) {
        for (UUID id : ACTIVE.values()) {
            PokemonEntity entity = find(server, id);
            if (entity != null) entity.discard();
        }
        ACTIVE.clear();
    }
}
