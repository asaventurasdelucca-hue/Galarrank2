package dev.galar.rank;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/** Client-only key binding: K summons or recalls the physical Cobblemon Rotom companion. */
public final class RotomClient {
    private static final KeyMapping OPEN_ROTOM = new KeyMapping(
            "key.galarrank.rotom", InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K, "key.categories.galarrank");

    private RotomClient() {}

    @EventBusSubscriber(modid = GalarRank.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
    public static final class Registration {
        @SubscribeEvent
        public static void register(RegisterKeyMappingsEvent event) {
            event.register(OPEN_ROTOM);
        }
    }

    @EventBusSubscriber(modid = GalarRank.MOD_ID, value = Dist.CLIENT)
    public static final class TickHandler {
        @SubscribeEvent
        public static void tick(ClientTickEvent.Post event) {
            while (OPEN_ROTOM.consumeClick()) {
                Minecraft minecraft = Minecraft.getInstance();
                if (minecraft.player != null && minecraft.player.connection != null) {
                    minecraft.player.connection.sendCommand("galar rotom");
                }
            }
        }
    }
}
