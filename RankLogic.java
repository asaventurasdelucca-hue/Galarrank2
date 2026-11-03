package dev.galar.rank;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class RankLogic {
    /** posicoes ganhas por tier de treinador de rota (tier 1..5) */
    // Cinco treinadores de rota somam 9900 posicoes: 10000 -> 100 (Ultra).
    // O treinador "teste" nao distribui pontos do campeonato.
    public static final int[] TIER_GAIN = {2800, 2200, 2000, 1700, 1200};
    public static final int LEADER_GAIN = 12;
    public static final int ULTRA_RANK = 100;
    public static final int MASTER_RANK = 8;

    private RankLogic() {}

    public static int classOf(int rank) {
        if (rank <= MASTER_RANK) return 2;
        if (rank <= ULTRA_RANK) return 1;
        return 0;
    }

    public static String className(int c) {
        return c == 2 ? "Master" : c == 1 ? "Ultra" : "Normal";
    }

    /** Chamado quando o jogador vence um treinador do ranking. Cada treinador pontua uma vez por jogador. */
    public static void onWin(ServerPlayer player, Trainers.Trainer t) {
        RankStore.PlayerRank pr = RankStore.get(player.getUUID());
        if (pr.defeated.contains(t.id())) {
            player.sendSystemMessage(Component.literal("[Rotom Phone] Voce ja pontuou contra " + t.name() + "."));
            return;
        }
        int before = classOf(pr.rank);
        int gain = "teste".equals(t.id()) ? 0
                : t.leader() ? LEADER_GAIN
                : (t.tier() >= 1 && t.tier() <= 5 ? TIER_GAIN[t.tier() - 1] : 0);
        pr.rank = Math.max(1, pr.rank - gain);
        pr.defeated.add(t.id());
        if (t.leader()) pr.badges++;
        if (t.champion()) pr.rank = 1;
        int after = classOf(pr.rank);
        RankStore.save();

        player.sendSystemMessage(Component.literal("[Rotom Phone] Vitoria contra " + t.name() + "! Rank: " + pr.rank
                + (gain > 0 ? " (subiu " + gain + " posicoes)" : "")));
        if (t.leader()) player.sendSystemMessage(Component.literal("Insignias: " + pr.badges + "/8"));
        if (after > before) {
            player.sendSystemMessage(Component.literal("[Rotom Phone] Sua classe subiu para " + className(after) + "!"));
        }
        if (t.champion() && player.getServer() != null) {
            player.getServer().getPlayerList().broadcastSystemMessage(
                    Component.literal(player.getGameProfile().getName() + " venceu Leon e e o novo Campeao de Galar!"), false);
        }
    }
}
