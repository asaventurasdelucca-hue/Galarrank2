package dev.galar.rank;

import com.cobblemon.mod.common.api.events.battles.BattleVictoryEvent;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.TrainerBattleActor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public final class BattleHooks {
    private BattleHooks() {}

    public static void onVictory(BattleVictoryEvent event) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (Object winner : event.getWinners()) {
            if (!(winner instanceof PlayerBattleActor playerActor)) continue;
            ServerPlayer player = server.getPlayerList().getPlayer(playerActor.getUuid());
            if (player == null) continue;
            for (Object loser : event.getLosers()) {
                if (!(loser instanceof TrainerBattleActor trainerActor)) continue;
                Trainers.Trainer trainer = Trainers.byName(trainerActor.getTrainerName());
                if (trainer != null) RankLogic.onWin(player, trainer);
            }
        }
    }
}
