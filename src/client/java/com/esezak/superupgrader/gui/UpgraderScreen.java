package com.esezak.superupgrader.gui;

import com.esezak.superupgrader.logic.SpinResult;
import com.esezak.superupgrader.logic.UpgradeCalculator;
import com.esezak.superupgrader.network.ModNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.esezak.superupgrader.gui.UpgraderStyle.*;

/** Reference-sized station with a modal catalog and server-authoritative wagers. */
public class UpgraderScreen extends AbstractContainerScreen<UpgraderScreenHandler> {
    private final WheelRenderer wheel = new WheelRenderer();
    private final List<Button> presets = new ArrayList<>();
    private Button upgradeButton;
    private Button targetButton;
    private Button decreaseButton;
    private Button increaseButton;
    private long cooldownEndsAt;
    private long requestSentAt;
    private boolean pickerOpen;

    public UpgraderScreen(UpgraderScreenHandler menu, Inventory inventory, Component title) {
        super(menu, inventory, Component.translatable("gui.superupgrader.title"),
                UpgraderScreenHandler.WIDTH, UpgraderScreenHandler.HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        pickerOpen = false;
        presets.clear();
        upgradeButton = addRenderableWidget(new StyledButton(leftPos + 8, topPos + 122, 82, 22,
                text("upgrade"), GOLD, button -> {
                    requestSentAt = System.currentTimeMillis();
                    ClientPlayNetworking.send(new ModNetworking.RequestSpinC2SPayload());
                    updateControls();
                }));
        upgradeButton.setTooltip(Tooltip.create(text("wager_hint")));
        targetButton = addRenderableWidget(new StyledButton(leftPos + 206, topPos + 96, 66, 14,
                text("browse"), GOLD, button -> openTargetSelection()));
        decreaseButton = addRenderableWidget(new StyledButton(leftPos + 206, topPos + 78, 16, 14,
                Component.literal("-"), MUTED, button -> setCount(menu.getTargetCount() - 1)));
        increaseButton = addRenderableWidget(new StyledButton(leftPos + 256, topPos + 78, 16, 14,
                Component.literal("+"), MUTED, button -> setCount(menu.getTargetCount() + 1)));

        String[] labels = {"×2", "×4", "×8", "30%", "50%", "70%"};
        double[] factors = {2, 4, 8, 0.30, 0.50, 0.70};
        for (int i = 0; i < labels.length; i++) {
            final int preset = i;
            Button button = new StyledButton(leftPos + 96 + i * 31, topPos + 122, 29, 22,
                    Component.literal(labels[i]), i < 3 ? GOLD : i == 3 ? RED : GREEN,
                    pressed -> applyPreset(preset < 3 ? inputValue() * factors[preset]
                            : UpgradeCalculator.HOUSE_EDGE * inputValue() / factors[preset]));
            button.setTooltip(Tooltip.create(text(i < 3 ? "preset_value" : "preset_chance", labels[i])));
            presets.add(addRenderableWidget(button));
        }
        updateControls();
    }

    private static Component text(String key, Object... args) {
        return Component.translatable("gui.superupgrader." + key, args);
    }

    public void onSpinResult(SpinResult result) {
        requestSentAt = 0;
        wheel.startSpin(result);
        updateControls();
    }

    public void setCooldownRemaining(long milliseconds) {
        cooldownEndsAt = System.currentTimeMillis() + milliseconds;
        requestSentAt = 0;
        updateControls();
    }

    public double inputValue() {
        return ClientCatalog.value(menu.getInputStack());
    }

    private boolean busy() {
        return wheel.isAnimating() || requestSentAt != 0;
    }

    private void setCount(int count) {
        if (busy()) return;
        menu.setTargetCount(count);
        ClientPlayNetworking.send(new ModNetworking.SetTargetCountC2SPayload(menu.getTargetCount()));
        updateControls();
    }

    private void selectTarget(Item item) {
        menu.setSelectedTarget(item);
        ClientPlayNetworking.send(new ModNetworking.SelectTargetC2SPayload(BuiltInRegistries.ITEM.getKey(item)));
        updateControls();
    }

    private void applyPreset(double desiredValue) {
        if (busy() || desiredValue <= 0) return;
        ClientCatalog.items().stream().min(Comparator.comparingDouble(item ->
                Math.abs(ClientCatalog.value(item) - desiredValue))).ifPresent(item -> {
                    selectTarget(item);
                    setCount(1);
                });
    }

    private void openTargetSelection() {
        if (busy()) return;
        pickerOpen = true;
        Minecraft.getInstance().setScreenAndShow(new TargetSelectionScreen(this, this::selectTarget, inputValue()));
    }

    private void updateControls() {
        if (upgradeButton == null) return;
        Item target = menu.getSelectedTarget();
        double wager = inputValue();
        double reward = ClientCatalog.value(target) * menu.getTargetCount();
        wheel.setChance(UpgradeCalculator.successChance(wager, reward));
        boolean ready = !busy();
        boolean coolingDown = System.currentTimeMillis() < cooldownEndsAt;
        upgradeButton.active = ready && !coolingDown && wager > 0 && reward > 0;
        upgradeButton.setMessage(busy() ? text("spinning") : coolingDown
                ? text("cooldown", (cooldownEndsAt - System.currentTimeMillis() + 999) / 1000) : text("upgrade"));
        targetButton.active = ready;
        decreaseButton.active = ready && target != null && menu.getTargetCount() > 1;
        increaseButton.active = ready && target != null && menu.getTargetCount() < menu.getMaxTargetCount();
        for (Button preset : presets) preset.active = ready && wager > 0 && !ClientCatalog.items().isEmpty();
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        wheel.tick();
        if (requestSentAt != 0 && System.currentTimeMillis() - requestSentAt > 5000) requestSentAt = 0;
        updateControls();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.fill(0, 0, width, height, 0xA0080A10);
        int x = leftPos;
        int y = topPos;
        panel(graphics, x, y, imageWidth, imageHeight, BORDER);
        graphics.centeredText(font, title, x + 144, y + 8, TEXT);
        drawCard(graphics, x + 8, y + 22);
        drawCard(graphics, x + 198, y + 22);
        slot(graphics, x + UpgraderScreenHandler.INPUT_X, y + UpgraderScreenHandler.INPUT_Y);
        slot(graphics, x + 230, y + 34);

        ItemStack input = menu.getInputStack();
        if (input.isEmpty()) {
            wrapped(graphics, text("pick_input"), x + 15, y + 56, 68, MUTED);
            wrapped(graphics, text("pick_input_hint"), x + 15, y + 82, 68, MUTED);
        } else {
            wrapped(graphics, input.getHoverName(), x + 15, y + 56, 68, TEXT);
            graphics.centeredText(font, text("value", number(inputValue())), x + 49, y + 96, GOLD);
            graphics.centeredText(font, text("wager_count", input.getCount()), x + 49, y + 82, MUTED);
        }

        Item target = menu.getSelectedTarget();
        if (target == null) {
            wrapped(graphics, text("pick_target"), x + 205, y + 56, 68, MUTED);
        } else {
            ItemStack stack = new ItemStack(target, menu.getTargetCount());
            graphics.item(stack, x + 230, y + 34);
            graphics.itemDecorations(font, stack, x + 230, y + 34);
            String name = font.plainSubstrByWidth(stack.getHoverName().getString(), 68);
            graphics.centeredText(font, name, x + 239, y + 55, TEXT);
            graphics.centeredText(font, text("value", number(ClientCatalog.value(target) * menu.getTargetCount())),
                    x + 239, y + 67, GOLD);
            if (mouseX >= x + 198 && mouseX < x + 280 && mouseY >= y + 22 && mouseY < y + 76) {
                graphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
            }
        }
        graphics.centeredText(font, Integer.toString(menu.getTargetCount()), x + 239, y + 81, TEXT);
        wheel.render(graphics, x + 144, y + 70, partialTick);
        graphics.text(font, playerInventoryTitle, x + UpgraderScreenHandler.INVENTORY_X, y + 149, MUTED, false);
        for (int i = 1; i < menu.slots.size(); i++) {
            var inventorySlot = menu.slots.get(i);
            slot(graphics, x + inventorySlot.x, y + inventorySlot.y);
        }
    }

    private void drawCard(GuiGraphicsExtractor graphics, int x, int y) {
        panel(graphics, x, y, 82, 94, BORDER);
        honeycomb(graphics, x, y, 82, 94);
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean wasHandled) {
        if (event.button() == 0 && event.x() >= leftPos + 198 && event.x() < leftPos + 280
                && event.y() >= topPos + 22 && event.y() < topPos + 76) {
            openTargetSelection();
            return true;
        }
        return super.mouseClicked(event, wasHandled);
    }

    private void wrapped(GuiGraphicsExtractor graphics, Component text, int x, int y, int width, int color) {
        var lines = font.split(text, width);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            graphics.centeredText(font, lines.get(i), x + width / 2, y + i * 10, color);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}

    @Override
    public void removed() {
        // Opening the catalog must not run the container's removal lifecycle.
        if (!pickerOpen) super.removed();
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
