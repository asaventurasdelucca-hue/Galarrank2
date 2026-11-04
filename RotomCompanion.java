package dev.galar.rank;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Interaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** A real 3D voxel drone, not a wild Rotom Pokemon (no drops, captures or battles). */
public final class RotomCompanion {
    public static final String DRONE_TAG = "galarrank_drone";
    private static final String PART_TAG = "galarrank_drone_part";
    public static final String OWNER_TAG_PREFIX = "galarrank_drone_owner_";

    private record Drone(UUID controller, List<UUID> parts) {}
    private static final Map<UUID, Drone> ACTIVE = new HashMap<>();

    public RotomCompanion() {}

    private static void vec(ListTag list, float... values) {
        for (float f : values) list.add(FloatTag.valueOf(f));
    }

    private static Display.BlockDisplay part(ServerLevel level, String block,
                                             float dx, float dy, float dz,
                                             float sx, float sy, float sz) {
        Display.BlockDisplay display = EntityType.BLOCK_DISPLAY.create(level);
        if (display == null) return null;
        CompoundTag blockState = new CompoundTag();
        blockState.putString("Name", block);
        CompoundTag transform = new CompoundTag();
        ListTag translation = new ListTag(); vec(translation, dx, dy, dz);
        ListTag scale = new ListTag(); vec(scale, sx, sy, sz);
        ListTag rotation = new ListTag(); vec(rotation, 0f, 0f, 0f, 1f);
        transform.put("translation", translation);
        transform.put("scale", scale);
        transform.put("left_rotation", rotation);
        transform.put("right_rotation", rotation.copy());
        CompoundTag data = new CompoundTag();
        data.put("block_state", blockState);
        data.put("transformation", transform);
        data.putInt("teleport_duration", 3);
        display.load(data);
        display.addTag(PART_TAG);
        return display;
    }

    private static void cleanupOld(ServerPlayer player) {
        // Remove any orphaned parts nearby left by an older session.
        for (Entity e : player.serverLevel().getEntities(player,
                new AABB(player.blockPosition()).inflate(48),
                entity -> entity.getTags().contains(OWNER_TAG_PREFIX + player.getUUID())
                       && (entity.getTags().contains(PART_TAG) || entity.getTags().contains(DRONE_TAG)))) {
            e.discard();
        }
    }

    private static Entity find(MinecraftServer server, UUID id) {
        if (server == null || id == null) return null;
        for (ServerLevel level : server.getAllLevels()) {
            Entity e = level.getEntity(id);
            if (e != null && !e.isRemoved()) return e;
        }
        return null;
    }

    private static void remove(MinecraftServer server, Drone d) {
        if (d == null) return;
        Entity center = find(server, d.controller());
        if (center != null) center.discard();
        for (UUID part : d.parts()) {
            Entity e = find(server, part);
            if (e != null) e.discard();
        }
    }

    /** true when newly summoned. */
    public static boolean toggle(ServerPlayer player) {
        UUID id = player.getUUID();
        Drone existing = ACTIVE.remove(id);
        if (existing != null && find(player.getServer(), existing.controller()) != null) {
            remove(player.getServer(), existing);
            player.displayClientMessage(Component.literal("§6Rotom Drone recolhido"), true);
            return false;
        }
        cleanupOld(player);
        ServerLevel level = player.serverLevel();
        try {
            Vec3 pos = player.position().add(1, 1.8, 1);
            Interaction controller = EntityType.INTERACTION.create(level);
            if (controller == null) return false;
            CompoundTag data = new CompoundTag();
            data.putFloat("width", 0.85F);
            data.putFloat("height", 0.7F);
            controller.load(data);
            controller.moveTo(pos.x, pos.y - 0.35, pos.z);
            controller.addTag(DRONE_TAG);
            controller.addTag(OWNER_TAG_PREFIX + id);
            controller.setCustomName(Component.literal("§6Rotom Drone"));
            controller.setCustomNameVisible(true);
            if (!level.addFreshEntity(controller)) return false;

            // Red shell, black screen, cyan camera and two little rotor arms.
            List<Display.BlockDisplay> models = List.of(
                part(level, "minecraft:red_concrete", -0.30F,-0.17F,-0.27F, 0.60F,0.38F,0.53F),
                part(level, "minecraft:black_concrete", -0.22F,-0.09F,-0.285F, 0.44F,0.24F,0.035F),
                part(level, "minecraft:light_blue_stained_glass", -0.085F,-0.04F,-0.30F, 0.17F,0.12F,0.035F),
                part(level, "minecraft:iron_block", -0.55F,0.11F,-0.10F, 1.1F,0.06F,0.20F),
                part(level, "minecraft:polished_blackstone", -0.66F,0.15F,-0.18F, 0.24F,0.045F,0.36F),
                part(level, "minecraft:polished_blackstone", 0.42F,0.15F,-0.18F, 0.24F,0.045F,0.36F)
            );
            List<UUID> uids = new ArrayList<>();
            for (Display.BlockDisplay display : models) {
                if (display == null) throw new IllegalStateException("Missing BlockDisplay");
                display.moveTo(pos);
                display.addTag(OWNER_TAG_PREFIX + id);
                if (!level.addFreshEntity(display)) throw new IllegalStateException("Cannot spawn BlockDisplay");
                uids.add(display.getUUID());
            }
            ACTIVE.put(id, new Drone(controller.getUUID(), List.copyOf(uids)));
            player.displayClientMessage(Component.literal("§6Rotom Drone ativado | Clique nele para ver o painel"), true);
            return true;
        } catch (Exception ex) {
            GalarRank.LOGGER.error("[GalarRank] Falha ao criar drone 3D", ex);
            cleanupOld(player);
            player.displayClientMessage(Component.literal("§cFalha ao invocar drone; veja latest.log"), true);
            return false;
        }
    }

    public static boolean isOwner(ServerPlayer player, Entity controller) {
        return controller.getTags().contains(DRONE_TAG)
                && controller.getTags().contains(OWNER_TAG_PREFIX + player.getUUID());
    }

    @SubscribeEvent
    public void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 3 != 0) return;
        for (var pair : new HashMap<>(ACTIVE).entrySet()) {
            ServerPlayer owner = server.getPlayerList().getPlayer(pair.getKey());
            Drone drone = pair.getValue();
            Entity control = find(server, drone.controller());
            if (owner == null || control == null || owner.level() != control.level()) {
                remove(server, drone);
                ACTIVE.remove(pair.getKey());
                continue;
            }
            double tick = server.getTickCount() / 11.0;
            Vec3 target = owner.position().add(1.0, 1.8 + Math.sin(tick) * 0.10, 0.85);
            Vec3 delta = target.subtract(control.position().add(0,0.35,0));
            Vec3 dest = delta.lengthSqr() > 64 ? target : control.position().add(0,0.35,0).add(delta.scale(0.40));
            control.teleportTo(dest.x, dest.y - 0.35, dest.z);
            for (UUID part : drone.parts()) {
                Entity e = find(server, part);
                if (e != null) e.teleportTo(dest.x, dest.y, dest.z);
            }
            if (server.getTickCount() % 90 == 0) RotomDrone.alert(owner);
        }
    }

    public static void clear(MinecraftServer server) {
        for (Drone d : ACTIVE.values()) remove(server, d);
        ACTIVE.clear();
    }
}
