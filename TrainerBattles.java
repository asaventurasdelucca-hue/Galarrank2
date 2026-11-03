package dev.galar.rank;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.BattleStartResult;
import com.cobblemon.mod.common.battles.ErroredBattleStart;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.actor.TrainerBattleActor;
import com.cobblemon.mod.common.battles.ai.StrongBattleAI;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TrainerBattles {
    private TrainerBattles() {}

    /** Retorna null se a batalha comecou, ou a mensagem de erro. */
    public static String start(ServerPlayer player, Trainers.Trainer t) {
        RankStore.PlayerRank pr = RankStore.get(player.getUUID());
        if (RankLogic.classOf(pr.rank) < t.reqClass()) {
            return "Voce precisa da Classe " + RankLogic.className(t.reqClass()) + " para desafiar " + t.name() + ".";
        }
        List<BattlePokemon> playerTeam = Cobblemon.INSTANCE.getStorage().getParty(player).toBattleTeam();
        if (playerTeam == null || playerTeam.isEmpty()) {
            return "Voce nao tem Pokemon aptos a lutar.";
        }
        List<BattlePokemon> npcTeam = new ArrayList<>();
        for (String spec : t.team()) {
            Pokemon pokemon = PokemonProperties.Companion.parse(spec).create();
            npcTeam.add(BattlePokemon.Companion.safeCopyOf(pokemon));
        }
        PlayerBattleActor playerActor = new PlayerBattleActor(player.getUUID(), playerTeam);
        TrainerBattleActor trainerActor = new TrainerBattleActor(t.name(), UUID.randomUUID(), npcTeam, new StrongBattleAI(t.skill()));
        BattleStartResult result = BattleRegistry.startBattle(
                BattleFormat.Companion.getGEN_9_SINGLES(),
                new BattleSide(playerActor),
                new BattleSide(trainerActor),
                false);
        if (result instanceof ErroredBattleStart) {
            return "Nao foi possivel iniciar a batalha (voce ja esta em uma?).";
        }
        return null;
    }
}
