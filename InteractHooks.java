package dev.galar.rank;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Clique direito em entidade marcada com galar_trainer_<id> inicia batalha. */
public final class InteractHooks {
    public static final String TAG_PREFIX = "galar_trainer_";

    /**
     * Intercepta antes do Entity#interactAt. O EntityInteract simples pode ser
     * tarde demais para NPCs que abrem uma interface durante o interactAt.
     */
    @SubscribeEvent
    public void onEntityInteract(PlayerInteractEvent.EntityInteractSpecific event) {
        if (event.getHand() != InteractionHand.MAIN_HAND) return;
        Entity target = event.getTarget();
        for (String tag : target.getTags()) {
            if (!tag.startsWith(TAG_PREFIX)) continue;
            Trainers.Trainer trainer = Trainers.ALL.get(tag.substring(TAG_PREFIX.length()));
            if (trainer == null) continue;

            // Cancela nos lados onde a tag estiver visivel. No servidor e
            // obrigatorio; algumas GUIs customizadas exigem integracao extra.
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.SUCCESS);
            if (!(event.getEntity() instanceof ServerPlayer player)) return;

            String error = TrainerBattles.start(player, trainer);
            if (error != null) {
                player.sendSystemMessage(Component.literal("[Rotom Phone] " + error));
            }
            return;
        }
    }
}
