package dev.galar.rank;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Clique direito em uma entidade marcada com a tag galar_trainer_<id> inicia a batalha contra esse treinador. */
public final class InteractHooks {
    public static final String TAG_PREFIX = "galar_trainer_";

    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Entity target = event.getTarget();
        for (String tag : target.getTags()) {
            if (!tag.startsWith(TAG_PREFIX)) continue;
            Trainers.Trainer trainer = Trainers.ALL.get(tag.substring(TAG_PREFIX.length()));
            if (trainer == null) continue;
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            String error = TrainerBattles.start(player, trainer);
            if (error != null) {
                player.sendSystemMessage(Component.literal("[Rotom Phone] " + error));
            }
            return;
        }
    }
}
