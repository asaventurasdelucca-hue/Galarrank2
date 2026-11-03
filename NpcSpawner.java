package dev.galar.rank;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.fml.ModList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Easy NPC integration: places NPCs from creator-authored skin presets.
 * No villagers, no placeholders, and no overwrite of authored NPC skin data.
 * The integration invokes Easy NPC's public preset command, so the GalarRank
 * compilation does not depend on Easy NPC's Java classes.
 */
public final class NpcSpawner {
    public static final String GENERATED_TAG = "galarrank_generated";
    private static final double SEARCH_RADIUS = 75;

    private NpcSpawner() {}

    private static boolean easyNpcAvailable() {
        return ModList.get().isLoaded("easy_npc");
    }

    /** Place one visual NPC from the world preset galar_<trainerId>. */
    public static String spawn(ServerPlayer player, Trainers.Trainer trainer) {
        if (!easyNpcAvailable()) return "Instale Easy NPC (NeoForge 1.21.1), ou crie NPC manualmente e use /galar admin bind.";
        BlockPos at = findGround(player.serverLevel(), player.blockPosition().offset(3,0,0));
        if (at == null) return "Nao achei terreno seguro perto de voce.";
        return importWorldPreset(player, at, trainer)
                ? null : "Preset ausente ou nao importado: galar_" + trainer.id()
                + ". Crie um NPC com skin, exporte como preset WORLD com esse nome e tente novamente.";
    }

    /** Places only presets that the server owner already created; safe for worlds with buildings. */
    public static int setup(ServerPlayer player) {
        if (!easyNpcAvailable()) return 0;
        int count = 0, route = 0, gym = 0;
        BlockPos origin = player.blockPosition();
        for (Trainers.Trainer trainer : Trainers.ALL.values()) {
            if (trainer.id().equals("teste") || hasNearby(player.serverLevel(), origin, trainer.id())) continue;
            int radius;
            double angle;
            if (trainer.champion()) { radius = 36; angle = Math.PI / 2; }
            else if (trainer.leader()) { radius = 28; angle = gym++ * (2 * Math.PI / 8); }
            else { radius = 14; angle = route++ * (2 * Math.PI / 5); }
            BlockPos spot = origin.offset((int)Math.round(radius*Math.cos(angle)), 0,
                    (int)Math.round(radius*Math.sin(angle)));
            BlockPos at = findGround(player.serverLevel(), spot);
            if (at != null && importWorldPreset(player, at, trainer)) count++;
        }
        return count;
    }

    /** Uses Easy NPC's delete command, never discards its entities directly. */
    public static int clear(ServerPlayer player) {
        if (!easyNpcAvailable()) return 0;
        List<Entity> nearby = player.serverLevel().getEntities(player,
                new AABB(player.blockPosition()).inflate(SEARCH_RADIUS),
                e -> e.getTags().contains(GENERATED_TAG) && isEasyNpc(e));
        int removed = 0;
        for (Entity npc : nearby) {
            try {
                player.getServer().getCommands().performPrefixedCommand(
                        player.createCommandSourceStack().withPermission(4),
                        "easy_npc delete " + npc.getUUID());
                // This command owns the saved Easy NPC index and can delete safely.
                if (npc.isRemoved()) removed++;
            } catch (Exception ex) {
                GalarRank.LOGGER.warn("[GalarRank] Unable to delete Easy NPC {}", npc.getUUID(), ex);
            }
        }
        return removed;
    }

    private static boolean hasNearby(ServerLevel level, BlockPos origin, String id) {
        return !level.getEntitiesOfClass(Entity.class, new AABB(origin).inflate(SEARCH_RADIUS),
                e -> e.getTags().contains(InteractHooks.TAG_PREFIX + id)).isEmpty();
    }

    private static boolean isEasyNpc(Entity e) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace().equals("easy_npc");
    }

    private static boolean importWorldPreset(ServerPlayer player, BlockPos pos, Trainers.Trainer trainer) {
        Set<UUID> before = new HashSet<>();
        for (Entity old : player.serverLevel().getEntities(player, new AABB(pos).inflate(4),
                NpcSpawner::isEasyNpc)) before.add(old.getUUID());
        String cmd = "easy_npc preset import_new world galar_" + trainer.id()
                + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
        try {
            player.getServer().getCommands().performPrefixedCommand(
                    player.createCommandSourceStack().withPermission(4).withPosition(pos.getCenter()), cmd);
            for (Entity entity : player.serverLevel().getEntities(player,
                    new AABB(pos).inflate(4), NpcSpawner::isEasyNpc)) {
                if (before.contains(entity.getUUID())) continue;
                entity.addTag(GENERATED_TAG);
                entity.addTag(InteractHooks.TAG_PREFIX + trainer.id());
                return true;
            }
        } catch (Exception ex) {
            GalarRank.LOGGER.error("[GalarRank] Easy NPC preset import error: {}", cmd, ex);
        }
        return false;
    }

    private static BlockPos findGround(ServerLevel level, BlockPos wanted) {
        for (int radius = 0; radius <= 3; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos column = wanted.offset(dx, 0, dz);
                    if (!level.hasChunkAt(column)) continue;
                    BlockPos at = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, column);
                    if (level.getWorldBorder().isWithinBounds(at)
                            && !level.getBlockState(at.below()).isAir()
                            && level.getFluidState(at.below()).isEmpty()
                            && level.getBlockState(at).isAir()
                            && level.getBlockState(at.above()).isAir()) return at;
                }
            }
