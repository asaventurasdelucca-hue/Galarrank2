package dev.galar.rank;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Radar: a compact client GUI for reports, action bar for nearby trainer alerts. */
public final class RotomDrone {
    public static final double RANGE = 64.0;
    private RotomDrone() {}

    private record Nearby(Trainers.Trainer trainer, double distance, int x, int z) {}

    private static List<Nearby> scan(ServerPlayer player) {
        List<Nearby> nearby = new ArrayList<>();
        for (Entity entity : player.serverLevel().getEntities(player,
                new AABB(player.blockPosition()).inflate(RANGE),
                e -> e.isAlive() && e.getTags().stream().anyMatch(tag -> tag.startsWith(InteractHooks.TAG_PREFIX)))) {
            for (String tag : entity.getTags()) {
                if (!tag.startsWith(InteractHooks.TAG_PREFIX)) continue;
                Trainers.Trainer trainer = Trainers.ALL.get(tag.substring(InteractHooks.TAG_PREFIX.length()));
                if (trainer == null) continue;
                double distance = Math.sqrt(entity.distanceToSqr(player));
                if (distance <= RANGE) nearby.add(new Nearby(trainer, distance,
                        entity.blockPosition().getX(), entity.blockPosition().getZ()));
                break;
            }
        }
        nearby.sort(Comparator.comparingDouble(Nearby::distance));
        return nearby;
    }

    public static void show(ServerPlayer player) {
        RankStore.PlayerRank rank = RankStore.get(player.getUUID());
        List<Nearby> nearby = scan(player);
        StringBuilder report = new StringBuilder();
        report.append("Rank #").append(rank.rank)
                .append("  |  ").append(RankLogic.className(RankLogic.classOf(rank.rank)))
                .append("  |  Pontos ").append(rank.points).append("\n")
                .append("Insignias: ").append(rank.badges).append("/8  |  Vitorias: ")
                .append(rank.defeated.size()).append("\n")
                .append("----------------------------------------------\n")
                .append("Treinadores ate 64 blocos:\n");
        if (nearby.isEmpty()) report.append("Nenhum treinador por perto.\n");
        for (int i = 0; i < Math.min(8, nearby.size()); i++) {
            Nearby n = nearby.get(i);
            String status = rank.defeated.contains(n.trainer.id()) ? "Vencido"
                    : RankLogic.classOf(rank.rank) < n.trainer.reqClass() ? "Bloqueado" : "Disponivel";
            report.append(n.trainer.name()).append(" - ").append((int)Math.round(n.distance))
                    .append("m  [").append(status).append("]\n");
        }
        if (nearby.size() > 8) report.append("+ ").append(nearby.size() - 8).append(" treinador(es)\n");
        PacketDistributor.sendToPlayer(player, new RotomReportPayload(report.toString()));
    }

    public static void alert(ServerPlayer player) {
        List<Nearby> nearby = scan(player);
        if (!nearby.isEmpty() && nearby.getFirst().distance() <= 24) {
            Nearby n = nearby.getFirst();
            player.displayClientMessage(Component.literal("§6Rotom Drone §f| Treinador: §b"
                    + n.trainer.name() + " §7(" + (int)Math.round(n.distance) + " blocos)"), true);
        }
    }
}
