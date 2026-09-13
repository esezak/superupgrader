package com.esezak.superupgrader.gui;

import com.esezak.superupgrader.logic.SpinResult;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import java.util.Locale;

/** Rasterizes a continuous annulus and keeps the server's winning sector fixed during a spin. */
public final class WheelRenderer {
    private static final int OUTER_RADIUS = 41;
    private static final int INNER_RADIUS = 30;
    private SpinResult result;
    private long startedAt;
    private double chance;
    private double startAngle;
    private double arc;
    private double needle;
    private boolean animating;

    public void setChance(double value) {
        if (animating || showingResult()) return;
        chance = value;
        arc = value * 360;
        startAngle = SpinResult.GOLD_ZONE_START_DEGREES;
        needle = 0;
    }

    public void startSpin(SpinResult spin) {
        result = spin;
        startedAt = System.currentTimeMillis();
        animating = true;
        chance = spin.chance();
        startAngle = spin.goldZoneStartAngle();
        arc = spin.goldZoneArcDegrees();
        needle = 0;
    }

    public boolean isAnimating() { return animating; }

    private boolean showingResult() {
        return result != null && !animating && System.currentTimeMillis() < startedAt + SpinResult.DURATION_MS + 1600;
    }

    public void tick() {
        if (!animating) return;
        double progress = Math.clamp((System.currentTimeMillis() - startedAt) / (double) SpinResult.DURATION_MS, 0, 1);
        needle = (1 - Math.pow(1 - progress, 3)) * (1800 + result.markerFinalAngle()) % 360;
        if (progress >= 1) {
            needle = result.markerFinalAngle();
            animating = false;
        }
    }

    public void render(GuiGraphicsExtractor graphics, int cx, int cy, float partialTick) {
        tick();
        drawRing(graphics, cx, cy);
        for (int tick = 0; tick < 60; tick++) {
            double angle = Math.toRadians(tick * 6 - 90);
            int inner = tick % 5 == 0 ? 43 : 45;
            UpgraderStyle.line(graphics, cx + (int) (Math.cos(angle) * inner), cy + (int) (Math.sin(angle) * inner),
                    cx + (int) (Math.cos(angle) * 47), cy + (int) (Math.sin(angle) * 47), 0xFF737D92);
        }
        // A narrow triangular needle, sampled along its length.
        double angle = Math.toRadians(needle - 90);
        for (int radius = 35; radius <= 50; radius++) {
            double halfWidth = (radius - 35) * 0.30;
            int x = cx + (int) (Math.cos(angle) * radius);
            int y = cy + (int) (Math.sin(angle) * radius);
            UpgraderStyle.line(graphics, x + (int) (-Math.sin(angle) * halfWidth), y + (int) (Math.cos(angle) * halfWidth),
                    x - (int) (-Math.sin(angle) * halfWidth), y - (int) (Math.cos(angle) * halfWidth), 0xFFFFE59A);
        }
        var font = Minecraft.getInstance().font;
        graphics.centeredText(font, String.format(Locale.ROOT, "%.2f%%", chance * 100), cx, cy - 8, UpgraderStyle.GOLD);
        graphics.centeredText(font, Component.translatable("gui.superupgrader.chance"), cx, cy + 5, UpgraderStyle.MUTED);
        if (showingResult()) {
            graphics.centeredText(font, Component.translatable("gui.superupgrader." + (result.success() ? "success" : "failure")),
                    cx, cy + 40, result.success() ? UpgraderStyle.GREEN : UpgraderStyle.RED);
        }
    }

    private void drawRing(GuiGraphicsExtractor graphics, int cx, int cy) {
        for (int y = -OUTER_RADIUS; y <= OUTER_RADIUS; y++) {
            int runStart = -OUTER_RADIUS;
            int previous = 0;
            for (int x = -OUTER_RADIUS; x <= OUTER_RADIUS + 1; x++) {
                int distanceSquared = x * x + y * y;
                int color = 0;
                if (x <= OUTER_RADIUS && distanceSquared <= OUTER_RADIUS * OUTER_RADIUS
                        && distanceSquared >= INNER_RADIUS * INNER_RADIUS) {
                    double degrees = (Math.toDegrees(Math.atan2(y, x)) + 450) % 360;
                    color = (degrees - startAngle + 360) % 360 < arc ? UpgraderStyle.GOLD : 0xFF343B4C;
                }
                if (color != previous) {
                    if (previous != 0) graphics.fill(cx + runStart, cy + y, cx + x, cy + y + 1, previous);
                    runStart = x;
                    previous = color;
                }
            }
        }
    }
}
