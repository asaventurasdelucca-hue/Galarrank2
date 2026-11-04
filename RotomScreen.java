package dev.galar.rank;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Compact non-pausing Rotom Drone status panel. Esc closes it, without chat spam. */
public final class RotomScreen extends Screen {
    private final String[] lines;

    public RotomScreen(String report) {
        super(Component.literal("Rotom Drone"));
        this.lines = report.split("\\n", -1);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics, mouseX, mouseY, partialTick);
        int panelW = Math.min(width - 24, 330);
        int panelH = Math.min(height - 24, 34 + lines.length * 13 + 22);
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;
        graphics.fill(x - 2, y - 2, x + panelW + 2, y + panelH + 2, 0xFFBB4422);
        graphics.fill(x, y, x + panelW, y + panelH, 0xEF10141D);
        graphics.fill(x + 9, y + 27, x + panelW - 9, y + 28, 0xFFBC663D);
        graphics.drawString(font, "ROTOM DRONE  /  GALARRANK", x + 12, y + 10, 0xFFFFA655, false);
        int lineY = y + 36;
        for (int i = 0; i < lines.length && lineY < y + panelH - 19; i++) {
            int color = i == 0 ? 0xFF79E0F8 : 0xFFECECEC;
            graphics.drawString(font, font.plainSubstrByWidth(lines[i], panelW - 22), x + 12, lineY, color, false);
            lineY += 13;
        }
        graphics.drawCenteredString(font, "ESC fecha  |  K recolhe o drone", width / 2, y + panelH - 14, 0xFFA9B6C1);
        super.render(graphics, mouseX, mouseY, partialTick);
    }
}
