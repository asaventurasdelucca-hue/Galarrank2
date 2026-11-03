package dev.galar.rank;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Server-side radar used by physical companion and /galar rotom info. */
public final class RotomDrone {
    public static final double RANGE = 64.0;
    private RotomDrone() {}

    public static void show(ServerPlayer player) {
        RankStore.PlayerRank rank = RankStore.get(player.getUUID());
        int completed = (int) rank.defeated.stream().filter(id -> !id.equals("teste")).count();
        player.sendSystemMessage(Component.literal("§6[Rotom Drone] §fRank #" + rank.rank
                + " | Classe: " + RankLogic.className(RankLogic.classOf(rank.rank))
                + " | Pontos: " + rank.points + " | Insignias: " + rank.badges + "/8 | Vitorias unicas: " + completed));
        List<Nearby> nearby = new ArrayList<>();
        for (Entity entity : player.serverLevel().getEntities(player,
                new AABB(player.blockPosition()).inflate(RANGE),
                e -> e.isAlive() && e.getTags().stream().anyMatch(tag -> tag.startsWith(InteractHooks.TAG_PREFIX)))) {
            for (String tag : entity.getTags()) {
                if (!tag.startsWith(InteractHooks.TAG_PREFIX)) continue;
                Trainers.Trainer trainer = Trainers.ALL.get(tag.substring(InteractHooks.TAG_PREFIX.length()));
                if (trainer == null) continue;
                double distance = Math.sqrt(entity.distanceToSqr(player));
                if (distance <= RANGE) nearby.add(new Nearby(entity, trainer, distance));
                break;
            }
        }
        nearby.sort(Comparator.comparingDouble(Nearby::distance));
        if (nearby.isEmpty()) {
            player.sendSystemMessage(Component.literal("§e[Rotom Drone] Nenhum treinador identificado em ate 64 blocos."));
            return;
        }
        player.sendSystemMessage(Component.literal("§e[Rotom Drone] Treinadores proximos (ate 64 blocos):"));
        for (int i = 0; i < Math.min(5, nearby.size()); i++) {
            Nearby n = nearby.get(i);
            String status = rank.defeated.contains(n.trainer.id()) ? "§aVencido"
                    : RankLogic.classOf(rank.rank) < n.trainer.reqClass() ? "§cBloqueado"
                    : "§bDisponivel";
            player.sendSystemMessage(Component.literal("§f- " + n.trainer.name() + " §7(" + (int) Math.round(n.distance)
                    + " blocos; X:" + n.entity.blockPosition().getX()
                    + " Z:" + n.entity.blockPosition().getZ() + ") - " + status));
        }
        if (nearby.size() > 5) player.sendSystemMessage(Component.literal("§7... e mais " + (nearby.size() - 5) + " treinador(es)."));
    }

    private record Nearby(Entity entity, Trainers.Trainer trainer, double distance) {}
}
