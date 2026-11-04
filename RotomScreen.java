package dev.galar.rank;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Rotom Drone radar panel without Minecraft's built-in background blur.
 * The translucent overlay keeps the world visible and sharp behind the panel.
 */
public final class RotomScreen extends Screen {
    private final String[] lines;

    public RotomScreen(String report) {
        super(Component.literal("Rotom Drone"));
        this.lines = report.split("\\n", -1);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Do not call renderBackground(): Minecraft 1.21.1 uses it to blur the world.
        // A plain translucent fill dims the scene without changing its sharpness.
        graphics.fill(0, 0, width, height, 0x44000000);

        int panelW = Math.min(width - 24, 400);
        int rowH = 16;
        int panelH = Math.min(height - 24, 48 + lines.length * rowH + 20);
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;

        graphics.fill(x - 2, y - 2, x + panelW + 2, y + panelH + 2, 0xFFFF8C3A);
        graphics.fill(x, y, x + panelW, y + panelH, 0xF5161B24);
        graphics.fill(x + 10, y + 29, x + panelW - 10, y + 30, 0xFFFF8C3A);
        graphics.drawString(font, "ROTOM DRONE  /  GALARRANK", x + 14, y + 11, 0xFFFFBF78, false);

        int lineY = y + 40;
        for (int i = 0; i < lines.length && lineY < y + panelH - 19; i++) {
            int color = i == 0 ? 0xFF78E1FF : 0xFFF3F5FA;
            graphics.drawString(font, font.plainSubstrByWidth(lines[i], panelW - 28),
                    x + 14, lineY, color, false);
            lineY += rowH;
        }
        graphics.drawCenteredString(font, "ESC para fechar  |  K para recolher o drone",
                width / 2, y + panelH - 15, 0xFFC1CCD7);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
