package dev.galar.rank;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.slf4j.Logger;

@Mod(GalarRank.MOD_ID)
public class GalarRank {
    public static final String MOD_ID = "galarrank";
    public static final Logger LOGGER = LogUtils.getLogger();

    public GalarRank(IEventBus modBus) {
        NeoForge.EVENT_BUS.register(this);
        NeoForge.EVENT_BUS.register(new InteractHooks());
        // vitoria em batalha do Cobblemon -> pontos de ranking
        CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.NORMAL, BattleHooks::onVictory);
        LOGGER.info("[GalarRank] carregado");
    }

    @SubscribeEvent
    public void onCommands(RegisterCommandsEvent event) {
        GalarCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        RankStore.load(event.getServer());
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        RankStore.save();
    }
}
