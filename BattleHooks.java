package dev.galar.rank;

import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.TrainerBattleActor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** So pontua vitorias contra o ator criado especificamente pelo GalarRank. */
public final class BattleHooks {
    private record PendingFight(UUID playerId, String trainerId, long createdAt) {}
    private static final Map<UUID, PendingFight> PENDING = new HashMap<>();
    private static final long EXPIRATION_MS = 2 * 60 * 60 * 1000L;

    private BattleHooks() {}

    public static void track(UUID trainerActorId, UUID playerId, String trainerId) {
        long now = System.currentTimeMillis();
        PENDING.entrySet().removeIf(e -> now - e.getValue().createdAt() > EXPIRATION_MS);
        PENDING.put(trainerActorId, new PendingFight(playerId, trainerId, now));
    }

    public static void clear() {
        PENDING.clear();
    }

    public static void onVictory(BattleVictoryEvent event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        server.execute(() -> handleVictory(server, event));
    }

    private static void handleVictory(MinecraftServer server, BattleVictoryEvent event) {
        // Remove qualquer batalha rastreada na qual o treinador venceu.
        for (Object winner : event.getWinners()) {
            if (winner instanceof TrainerBattleActor trainerActor) {
                PENDING.remove(trainerActor.getUuid());
            }
        }

        for (Object loser : event.getLosers()) {
            if (!(loser instanceof TrainerBattleActor trainerActor)) continue;
            PendingFight pending = PENDING.remove(trainerActor.getUuid());
            if (pending == null) continue;
            for (Object winner : event.getWinners()) {
                if (!(winner instanceof PlayerBattleActor playerActor)) continue;
                if (!pending.playerId().equals(playerActor.getUuid())) continue;
                ServerPlayer player = server.getPlayerList().getPlayer(pending.playerId());
                Trainers.Trainer trainer = Trainers.ALL.get(pending.trainerId());
                if (player != null && trainer != null) {
                    RankLogic.onWin(player, trainer);
                }
                break;
            }
        }
    }
}
