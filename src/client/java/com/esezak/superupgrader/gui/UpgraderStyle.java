package com.esezak.superupgrader.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import java.text.NumberFormat;

/** Shared charcoal, warm-gold and beveled surfaces from the reference interface. */
public final class UpgraderStyle {
    public static final int TEXT = 0xFFE6EAF2;
    public static final int MUTED = 0xFF8790A3;
    public static final int GOLD = 0xFFF2C94C;
    public static final int BORDER = 0xFF39405A;
    public static final int GREEN = 0xFF8AC694;
    public static final int RED = 0xFFE4938B;

    private UpgraderStyle() {}

    public static String number(double value) {
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(value < 10 ? 2 : 1);
        return format.format(value);
    }

    public static void panel(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int border) {
        graphics.fill(x + 2, y, x + width - 2, y + height, border);
        graphics.fill(x, y + 2, x + width, y + height - 2, border);
        for (int row = 1; row < height - 1; row++) {
            int inset = row == 1 || row == height - 2 ? 2 : 1;
            int shade = 36 - row * 10 / height;
            int color = 0xFF000000 | shade << 16 | (shade + 3) << 8 | (shade + 13);
            graphics.fill(x + inset, y + row, x + width - inset, y + row + 1, color);
        }
    }

    public static void slot(GuiGraphicsExtractor graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, BORDER);
        graphics.fill(x, y, x + 16, y + 16, 0xFF0B0E17);
        graphics.fill(x, y, x + 16, y + 1, 0xFF070910);
    }

    public static void line(GuiGraphicsExtractor graphics, int x1, int y1, int x2, int y2, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int i = 0; i <= steps; i++) {
            int x = x1 + (x2 - x1) * i / Math.max(1, steps);
            int y = y1 + (y2 - y1) * i / Math.max(1, steps);
            graphics.fill(x, y, x + 1, y + 1, color);
        }
    }

    public static void honeycomb(GuiGraphicsExtractor graphics, int x, int y, int width, int height) {
        graphics.enableScissor(x + 2, y + 2, x + width - 2, y + height - 2);
        for (int row = 0; row < height / 12 + 2; row++) {
            for (int col = 0; col < width / 20 + 2; col++) {
                int cx = x + col * 20 + (row % 2) * 10;
                int cy = y + row * 12;
                int[] dx = {-8, -4, 4, 8, 4, -4};
                int[] dy = {0, -6, -6, 0, 6, 6};
                for (int i = 0; i < 6; i++) {
                    int next = (i + 1) % 6;
                    line(graphics, cx + dx[i], cy + dy[i], cx + dx[next], cy + dy[next], 0xFF2A2F3D);
                }
            }
        }
        graphics.disableScissor();
    }

    public static final class StyledButton extends Button {
        private final int accent;

        public StyledButton(int x, int y, int width, int height, Component text, int accent, OnPress action) {
            super(x, y, width, height, text, action, DEFAULT_NARRATION);
            this.accent = accent;
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            int color = active ? accent : MUTED;
            panel(graphics, getX(), getY(), getWidth(), getHeight(), active && isHoveredOrFocused() ? color : BORDER);
            if (active) graphics.fill(getX() + 3, getY() + getHeight() - 2, getX() + getWidth() - 3, getY() + getHeight() - 1, color);
            graphics.centeredText(Minecraft.getInstance().font, getMessage(),
                    getX() + getWidth() / 2, getY() + (getHeight() - 8) / 2, color);
        }
    }
}
