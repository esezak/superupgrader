package com.esezak.superupgrader.gui;

import com.esezak.superupgrader.logic.UpgradeCalculator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import static com.esezak.superupgrader.gui.UpgraderStyle.*;

/** The reference's 14-column item catalog, presented as a modal over the open container. */
public class TargetSelectionScreen extends net.minecraft.client.gui.screens.Screen {
    private static final int COLUMNS = 14;
    private static final int ROWS = 9;
    private static final int CELL = 18;
    private static final int WIDTH = 272;
    private static final int HEIGHT = 224;
    private final UpgraderScreen parent;
    private final Consumer<Item> onSelect;
    private final double inputValue;
    private final List<CatalogButton> cells = new ArrayList<>();
    private List<Item> filtered = List.of();
    private EditBox search;
    private int left;
    private int top;
    private int scrollRow;
    private int catalogRevision = -1;

    public TargetSelectionScreen(UpgraderScreen parent, Consumer<Item> onSelect, double inputValue) {
        super(Component.translatable("gui.superupgrader.select_target"));
        this.parent = parent;
        this.onSelect = onSelect;
        this.inputValue = inputValue;
    }

    @Override
    protected void init() {
        left = (width - WIDTH) / 2;
        top = (height - HEIGHT) / 2;
        cells.clear();
        String previous = search == null ? "" : search.getValue();
        search = addRenderableWidget(new EditBox(font, left + 10, top + 23, 232, 16,
                Component.translatable("gui.superupgrader.search")));
        search.setHint(Component.translatable("gui.superupgrader.search"));
        search.setValue(previous);
        search.setResponder(this::filter);
        addRenderableWidget(new StyledButton(left + 246, top + 23, 16, 16, Component.literal("×"), MUTED,
                button -> onClose()));
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < COLUMNS; col++) {
                cells.add(addRenderableWidget(new CatalogButton(left + 10 + col * CELL, top + 44 + row * CELL)));
            }
        }
        filter(previous);
        setInitialFocus(search);
    }

    private void filter(String query) {
        String needle = query.strip().toLowerCase(Locale.ROOT);
        filtered = ClientCatalog.items().stream().filter(item ->
                new ItemStack(item).getHoverName().getString().toLowerCase(Locale.ROOT).contains(needle)
                        || BuiltInRegistries.ITEM.getKey(item).toString().contains(needle)).toList();
        scrollRow = 0;
        catalogRevision = ClientCatalog.revision();
        refreshCells();
    }

    private int maxScrollRow() {
        return Math.max(0, (filtered.size() + COLUMNS - 1) / COLUMNS - ROWS);
    }

    private void refreshCells() {
        for (int index = 0; index < cells.size(); index++) {
            CatalogButton button = cells.get(index);
            int itemIndex = scrollRow * COLUMNS + index;
            button.item = itemIndex < filtered.size() ? filtered.get(itemIndex) : null;
            button.active = button.item != null;
            button.setMessage(button.item == null ? Component.empty() : new ItemStack(button.item).getHoverName());
        }
    }

    @Override
    public void tick() {
        if (ClientCatalog.revision() != catalogRevision) filter(search.getValue());
        // Keep the underlying container synchronized while the picker is open.
        parent.tick();
        if (minecraft.player == null || minecraft.player.containerMenu != parent.getMenu()) minecraft.setScreenAndShow(null);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        parent.extractRenderState(graphics, -1, -1, partialTick);
        graphics.fill(0, 0, width, height, 0xC0080A10);
        panel(graphics, left, top, WIDTH, HEIGHT, GOLD);
        graphics.centeredText(font, title, left + WIDTH / 2, top + 8, TEXT);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        if (filtered.isEmpty()) {
            graphics.centeredText(font, Component.translatable("gui.superupgrader.no_results"),
                    left + WIDTH / 2, top + 112, MUTED);
        }
        graphics.centeredText(font, Component.translatable("gui.superupgrader.picker_footer",
                filtered.size(), scrollRow + 1, maxScrollRow() + 1), left + WIDTH / 2, top + 212, MUTED);
        if (maxScrollRow() > 0) {
            int thumbY = top + 44 + scrollRow * 146 / maxScrollRow();
            graphics.fill(left + 264, top + 44, left + 266, top + 206, BORDER);
            graphics.fill(left + 264, thumbY, left + 266, thumbY + 16, GOLD);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (vertical == 0) return false;
        scrollRow = Math.clamp(scrollRow - (int) Math.signum(vertical), 0, maxScrollRow());
        refreshCells();
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean wasHandled) {
        if (event.x() < left || event.x() >= left + WIDTH || event.y() < top || event.y() >= top + HEIGHT) {
            onClose();
            return true;
        }
        return super.mouseClicked(event, wasHandled);
    }

    @Override
    public void onClose() {
        parent.init(width, height);
        Minecraft.getInstance().setScreenAndShow(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private class CatalogButton extends Button {
        private Item item;

        CatalogButton(int x, int y) {
            super(x, y, CELL, CELL, Component.empty(), button -> {
                Item selected = ((CatalogButton) button).item;
                if (selected != null) {
                    onSelect.accept(selected);
                    onClose();
                }
            }, DEFAULT_NARRATION);
        }

        @Override
        protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            graphics.fill(getX(), getY(), getX() + CELL - 1, getY() + CELL - 1,
                    isHoveredOrFocused() && active ? 0xFF5A5134 : 0xFF10141F);
            if (item == null) return;
            ItemStack stack = new ItemStack(item);
            graphics.item(stack, getX() + 1, getY() + 1);
            if (isHoveredOrFocused()) {
                double value = ClientCatalog.value(item);
                graphics.setComponentTooltipForNextFrame(font, List.of(stack.getHoverName(),
                        Component.literal(BuiltInRegistries.ITEM.getKey(item).toString()).withColor(MUTED),
                        Component.translatable("gui.superupgrader.value", number(value)).withColor(GOLD),
                        Component.translatable("gui.superupgrader.estimate",
                                UpgradeCalculator.formatChance(UpgradeCalculator.successChance(inputValue, value))).withColor(GREEN)),
                        mouseX, mouseY);
            }
        }
    }
}
