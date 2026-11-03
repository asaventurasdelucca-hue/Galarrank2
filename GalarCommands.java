package dev.galar.rank;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class GalarCommands {
    private GalarCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("galar")
            .then(Commands.literal("rank").executes(ctx -> showRank(ctx.getSource())))
            .then(Commands.literal("trainers").executes(ctx -> listTrainers(ctx.getSource())))
            .then(Commands.literal("rotom")
                .executes(ctx -> toggleRotom(ctx.getSource()))
                .then(Commands.literal("info").executes(ctx -> showRotom(ctx.getSource()))))
            .then(Commands.literal("battle")
                .then(Commands.argument("trainer", StringArgumentType.word())
                    .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(Trainers.ALL.keySet(), builder))
                    .executes(ctx -> battle(ctx.getSource(), StringArgumentType.getString(ctx, "trainer")))))
            .then(Commands.literal("admin")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("setrank")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("rank", IntegerArgumentType.integer(1, 100000))
                            .executes(ctx -> setRank(ctx.getSource(),
                                    EntityArgument.getPlayer(ctx, "player"),
                                    IntegerArgumentType.getInteger(ctx, "rank"))))))
                .then(Commands.literal("reset")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> reset(ctx.getSource(), EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("bind")
                    .then(Commands.argument("trainer", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(Trainers.ALL.keySet(), builder))
                        .executes(ctx -> bind(ctx.getSource(), StringArgumentType.getString(ctx, "trainer")))))
                .then(Commands.literal("spawn")
                    .then(Commands.argument("trainer", StringArgumentType.word())
                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(Trainers.ALL.keySet(), builder))
                        .executes(ctx -> spawn(ctx.getSource(), StringArgumentType.getString(ctx, "trainer")))))
                .then(Commands.literal("setup").executes(ctx -> setup(ctx.getSource())))
                .then(Commands.literal("clearsetup").executes(ctx -> clearSetup(ctx.getSource())))
                .then(Commands.literal("unbind").executes(ctx -> unbind(ctx.getSource())))
                .then(Commands.literal("win")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("trainer", StringArgumentType.word())
                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(Trainers.ALL.keySet(), builder))
                            .executes(ctx -> fakeWin(ctx.getSource(),
                                    EntityArgument.getPlayer(ctx, "player"),
                                    StringArgumentType.getString(ctx, "trainer"))))))));
    }


    private static int toggleRotom(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer player = src.getPlayerOrException();
        if (RotomCompanion.toggle(player)) RotomDrone.show(player);
        return 1;
    }

    private static int showRotom(CommandSourceStack src) throws CommandSyntaxException {
        RotomDrone.show(src.getPlayerOrException());
        return 1;
    }

    private static int spawn(CommandSourceStack src, String id) throws CommandSyntaxException {
        Trainers.Trainer trainer = Trainers.ALL.get(id);
        if (trainer == null) {
            src.sendFailure(Component.literal("Treinador desconhecido: " + id));
            return 0;
        }
        String error = NpcSpawner.spawn(src.getPlayerOrException(), trainer);
        if (error != null) {
            src.sendFailure(Component.literal("[GalarRank] " + error));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("[GalarRank] NPC de " + trainer.name()
                + " criado a partir do preset Easy NPC. Clique direito nele para desafiar."), true);
        return 1;
    }

    private static int setup(CommandSourceStack src) throws CommandSyntaxException {
        int spawned = NpcSpawner.setup(src.getPlayerOrException());
        src.sendSuccess(() -> Component.literal("[GalarRank] Treinadores criados a partir de presets Easy NPC: "
                + spawned + ". Use K para localizar os NPCs."), true);
        return spawned > 0 ? 1 : 0;
    }

    private static int clearSetup(CommandSourceStack src) throws CommandSyntaxException {
        int removed = NpcSpawner.clear(src.getPlayerOrException());
        src.sendSuccess(() -> Component.literal("[GalarRank] NPCs Easy NPC removidos: " + removed), true);
        return 1;
    }

    private static int showRank(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        RankStore.PlayerRank pr = RankStore.get(p.getUUID());
        String msg = "[Rotom Phone] Rank: " + pr.rank + " | Classe: " + RankLogic.className(RankLogic.classOf(pr.rank))
                + " | Pontos: " + pr.points + " | Insignias: " + pr.badges + "/8 | Treinadores vencidos: " + pr.defeated.size();
        src.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int listTrainers(CommandSourceStack src) {
        StringBuilder sb = new StringBuilder("[Rotom Phone] Treinadores: ");
        for (Trainers.Trainer t : Trainers.ALL.values()) {
            sb.append(t.id()).append("(").append(RankLogic.className(t.reqClass())).append(") ");
        }
        String msg = sb.toString();
        src.sendSuccess(() -> Component.literal(msg), false);
        return 1;
    }

    private static int battle(CommandSourceStack src, String id) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        Trainers.Trainer t = Trainers.ALL.get(id);
        if (t == null) {
            src.sendFailure(Component.literal("Treinador desconhecido: " + id));
            return 0;
        }
        String error = TrainerBattles.start(p, t);
        if (error != null) {
            src.sendFailure(Component.literal("[Rotom Phone] " + error));
            return 0;
        }
        return 1;
    }

    private static int setRank(CommandSourceStack src, ServerPlayer target, int rank) {
        RankStore.get(target.getUUID()).rank = rank;
        RankStore.save();
        src.sendSuccess(() -> Component.literal("Rank de " + target.getGameProfile().getName() + " = " + rank), true);
        return 1;
    }

    private static int reset(CommandSourceStack src, ServerPlayer target) {
        RankStore.reset(target.getUUID());
        src.sendSuccess(() -> Component.literal("Rank de " + target.getGameProfile().getName() + " zerado."), true);
        return 1;
    }

    /**
     * Escolhe a entidade viva para a qual o admin esta olhando, num alcance de
     * ate cinco blocos. Isso evita marcar sem querer um Pokemon ao lado.
     */
    private static Entity nearestNpc(ServerPlayer p) {
        Vec3 eye = p.getEyePosition();
        Vec3 view = p.getLookAngle();
        List<Entity> list = p.level().getEntities(p, p.getBoundingBox().inflate(5.0),
                e -> e instanceof LivingEntity && !(e instanceof Player) && e.isAlive());
        return list.stream().filter(e -> {
            Vec3 toward = e.getBoundingBox().getCenter().subtract(eye);
            double distance = toward.length();
            return distance > 0.0 && distance <= 5.0
                    && view.dot(toward.scale(1.0 / distance)) > 0.97
                    && p.hasLineOfSight(e);
        }).min(Comparator.comparingDouble(e -> e.distanceToSqr(p))).orElse(null);
    }

    private static void clearTrainerTags(Entity target) {
        for (String tag : new ArrayList<>(target.getTags())) {
            if (tag.startsWith(InteractHooks.TAG_PREFIX)) target.removeTag(tag);
        }
    }

    private static int bind(CommandSourceStack src, String id) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        if (!Trainers.ALL.containsKey(id)) {
            src.sendFailure(Component.literal("Treinador desconhecido: " + id));
            return 0;
        }
        Entity target = nearestNpc(p);
        if (target == null) {
            src.sendFailure(Component.literal("Olhe para o NPC a ate 5 blocos e tente novamente."));
            return 0;
        }
        clearTrainerTags(target);
        target.addTag(InteractHooks.TAG_PREFIX + id);
        src.sendSuccess(() -> Component.literal("NPC marcado como " + id + ". Clique direito nele para batalhar."), true);
        return 1;
    }

    private static int unbind(CommandSourceStack src) throws CommandSyntaxException {
        ServerPlayer p = src.getPlayerOrException();
        Entity target = nearestNpc(p);
        if (target == null) {
            src.sendFailure(Component.literal("Olhe para o NPC a ate 5 blocos e tente novamente."));
            return 0;
        }
        clearTrainerTags(target);
        src.sendSuccess(() -> Component.literal("Marca de treinador removida."), true);
        return 1;
    }

    private static int fakeWin(CommandSourceStack src, ServerPlayer target, String id) {
        Trainers.Trainer t = Trainers.ALL.get(id);
        if (t == null) {
            src.sendFailure(Component.literal("Treinador desconhecido: " + id));
            return 0;
        }
        RankLogic.onWin(target, t);
        return 1;
    }
}
