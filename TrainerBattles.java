package dev.galar.rank;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.battles.BattleFormat;
import com.cobblemon.mod.common.battles.BattleRegistry;
import com.cobblemon.mod.common.battles.BattleSide;
import com.cobblemon.mod.common.battles.BattleStartResult;
import com.cobblemon.mod.common.battles.SuccessfulBattleStart;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.battles.ai.StrongBattleAI;
import com.cobblemon.mod.common.battles.pokemon.BattlePokemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class TrainerBattles {
    private TrainerBattles() {}

    /** Returns null when the battle starts; provides useful diagnostics otherwise. */
    public static String start(ServerPlayer player, Trainers.Trainer trainer) {
        return start(player, trainer, (Entity) null);
    }

    /** Uses the physical NPC for battle positioning, or a point in front of the player for /galar battle. */
    public static String start(ServerPlayer player, Trainers.Trainer trainer, Entity npc) {
        RankStore.PlayerRank ranking = RankStore.get(player.getUUID());
        if (RankLogic.classOf(ranking.rank) < trainer.reqClass()) {
            return "Voce precisa da Classe " + RankLogic.className(trainer.reqClass()) + " para desafiar " + trainer.name() + ".";
        }
        if (BattleRegistry.getBattleByParticipatingPlayer(player) != null) {
            return "Voce ja esta em uma batalha.";
        }
        List<BattlePokemon> playerTeam = Cobblemon.INSTANCE.getStorage().getParty(player).toBattleTeam();
        if (playerTeam == null || playerTeam.isEmpty()) {
            return "Voce nao tem Pokemon aptos a lutar. Cure seu time.";
        }

        List<BattlePokemon> enemyTeam = new ArrayList<>();
        StringBuilder names = new StringBuilder();
        for (String spec : trainer.team()) {
            try {
                Pokemon pokemon = PokemonProperties.Companion.parse(spec).create();
                if (pokemon == null || pokemon.getSpecies() == null) {
                    return "Time invalido de " + trainer.name() + ": " + spec + " (especie ausente).";
                }
                // A new Pokemon's battle copy can otherwise carry zero HP in some versions.
                pokemon.heal();
                BattlePokemon battleCopy = BattlePokemon.Companion.safeCopyOf(pokemon);
                battleCopy.getEffectedPokemon().heal();
                if (battleCopy.getEffectedPokemon().getCurrentHealth() <= 0) {
                    return "Pokemon sem HP no time de " + trainer.name() + ": " + spec;
                }
                enemyTeam.add(battleCopy);
                if (!names.isEmpty()) names.append(", ");
                names.append(spec);
            } catch (Exception ex) {
                GalarRank.LOGGER.error("[GalarRank] erro ao criar Pokemon do treinador {}: {}", trainer.id(), spec, ex);
                return "Erro criando Pokemon " + spec + ". Veja latest.log para detalhes.";
            }
        }
        if (enemyTeam.size() != trainer.team().size() || enemyTeam.isEmpty()) {
            return "Time invalido para " + trainer.name() + " (esperado " + trainer.team().size()
                    + ", criado " + enemyTeam.size() + ").";
        }

        GalarRank.LOGGER.info("[GalarRank] Iniciando PvE {}: jogador={} Pokemon, treinador={} Pokemon [{}]",
                trainer.id(), playerTeam.size(), enemyTeam.size(), names);
        PlayerBattleActor playerActor = new PlayerBattleActor(player.getUUID(), playerTeam);
        // Cobblemon only knows where to send out an AI trainer's Pokemon when the
        // actor implements EntityBackedBattleActor. TrainerBattleActor does NOT.
        LivingEntity anchor = npc instanceof LivingEntity living ? living : player;
        Vec3 opponentPos;
        if (npc != null) {
            opponentPos = npc.position();
        } else {
            Vec3 look = player.getLookAngle();
            Vec3 horizontal = new Vec3(look.x, 0, look.z);
            if (horizontal.lengthSqr() < 0.001) horizontal = new Vec3(0, 0, 1);
            opponentPos = player.position().add(horizontal.normalize().scale(6));
        }
        // Avoid zero-length actor distance if the NPC intersects the player.
        if (opponentPos.distanceToSqr(player.position()) < 1.0) {
            Vec3 look = player.getLookAngle();
            Vec3 horizontal = new Vec3(look.x, 0, look.z);
            if (horizontal.lengthSqr() < 0.001) horizontal = new Vec3(0, 0, 1);
            opponentPos = player.position().add(horizontal.normalize().scale(6));
        }
        UUID actorId = npc != null ? npc.getUUID() : UUID.randomUUID();
        GalarTrainerBattleActor trainerActor = new GalarTrainerBattleActor(
                trainer.name(), actorId, enemyTeam, new StrongBattleAI(trainer.skill()), anchor, opponentPos);
        GalarRank.LOGGER.info("[GalarRank] Adversario {} ancorado em {}, Pokemon={}",
                trainer.id(), opponentPos, trainerActor.getPokemonList().size());
        BattleStartResult result;
        GalarLoot.mark(enemyTeam);
        try {
            result = BattleRegistry.startBattle(
                    BattleFormat.Companion.getGEN_9_SINGLES(),
                    new BattleSide(playerActor),
                    new BattleSide(trainerActor),
                    false);
        } catch (Exception ex) {
            GalarRank.LOGGER.error("[GalarRank] Falha ao iniciar batalha com {}", trainer.id(), ex);
            GalarLoot.unmark(enemyTeam);
            return "Erro ao iniciar batalha. Veja latest.log.";
        }
        if (!(result instanceof SuccessfulBattleStart)) {
            GalarRank.LOGGER.warn("[GalarRank] BattleRegistry nao iniciou batalha com {}: {}", trainer.id(), result);
            GalarLoot.unmark(enemyTeam);
            return "Batalha rejeitada pelo Cobblemon. Verifique latest.log.";
        }
        BattleHooks.track(actorId, player.getUUID(), trainer.id());
        return null;
    }
}
