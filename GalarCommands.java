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
                .then(Commands.literal("delete")
                    .then(Commands.literal("confirm").executes(ctx -> deleteBoundNpc(ctx.getSource()))))
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
