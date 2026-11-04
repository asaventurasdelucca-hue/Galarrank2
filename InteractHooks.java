package dev.galar.rank;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Starts a battle from a right-click on a marked trainer NPC. */
public final class InteractHooks {
    public static final String TAG_PREFIX = "galar_trainer_";
    private static final Map<UUID, Long> RECENT = new HashMap<>();
    private static final long CLICK_COOLDOWN_MS = 1400;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void specific(PlayerInteractEvent.EntityInteractSpecific event) {
        handle(event.getEntity(), event.getHand(), event.getTarget(), () -> {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        });
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void normal(PlayerInteractEvent.EntityInteract event) {
        handle(event.getEntity(), event.getHand(), event.getTarget(), () -> {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
        });
    }

    private static void handle(net.minecraft.world.entity.player.Player source,
                               InteractionHand hand, Entity target, Runnable cancel) {
        if (hand != InteractionHand.MAIN_HAND) return;
        // Click on the physical Rotom to refresh the report, not to battle it.
        if (target.getTags().contains(RotomCompanion.DRONE_TAG)) {
            cancel.run();
            if (source instanceof ServerPlayer player && RotomCompanion.isOwner(player, target)) {
                RotomDrone.show(player);
            }
            return;
        }
        for (String tag : target.getTags()) {
            if (!tag.startsWith(TAG_PREFIX)) continue;
            Trainers.Trainer trainer = Trainers.ALL.get(tag.substring(TAG_PREFIX.length()));
            if (trainer == null) continue;
            // Also cancel the vanilla villager trading screen.
            cancel.run();
            if (!(source instanceof ServerPlayer player)) return;

            long now = System.currentTimeMillis();
            Long previous = RECENT.put(player.getUUID(), now);
            if (previous != null && now - previous < CLICK_COOLDOWN_MS) return;

            String error = TrainerBattles.start(player, trainer, target);
            if (error != null) player.sendSystemMessage(Component.literal("[Rotom Phone] " + error));
            else player.sendSystemMessage(Component.literal("§6[Rotom Drone] §fDesafiando " + trainer.name() + "!"));
            return;
        }
    }
}
