package com.esezak.superupgrader.gui;

import com.esezak.superupgrader.SuperUpgrader;
import com.esezak.superupgrader.logic.CooldownTracker;
import com.esezak.superupgrader.logic.SpinResult;
import com.esezak.superupgrader.logic.UpgradeCalculator;
import com.esezak.superupgrader.value.ItemValueCache;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Random;

/** Owns the wager and reward; only the input slot is consumed by an upgrade. */
public class UpgraderScreenHandler extends AbstractContainerMenu {
    public static MenuType<UpgraderScreenHandler> SCREEN_HANDLER_TYPE;
    public static final int WIDTH = 288;
    public static final int HEIGHT = 240;
    public static final int INPUT_X = 40;
    public static final int INPUT_Y = 34;
    public static final int INVENTORY_X = 63;
    public static final int INVENTORY_Y = 160;
    public static final int HOTBAR_Y = 218;

    private final UpgraderInventory input;
    private final Player player;
    private final DataSlot targetId = DataSlot.standalone();
    private final DataSlot targetCount = DataSlot.standalone();
    private long spinEndsAt;
    private ItemStack pendingReward = ItemStack.EMPTY;

    public static void register() {
        SCREEN_HANDLER_TYPE = Registry.register(BuiltInRegistries.MENU, SuperUpgrader.id("upgrader"),
                new MenuType<>(UpgraderScreenHandler::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
    }

    public UpgraderScreenHandler(int syncId, Inventory inventory) {
        super(SCREEN_HANDLER_TYPE, syncId);
        this.input = new UpgraderInventory();
        this.player = inventory.player;
        targetCount.set(1);
        addDataSlot(targetId);
        addDataSlot(targetCount);
        addSlot(new Slot(input, 0, INPUT_X, INPUT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                // The client has a separate server-supplied catalog; server validation is authoritative.
                return !isSpinning() && (player.level().isClientSide() || ItemValueCache.hasValue(stack.getItem()));
            }

            @Override
            public boolean mayPickup(Player player) {
                return !isSpinning();
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9,
                        INVENTORY_X + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, INVENTORY_X + col * 18, HOTBAR_Y));
        }
    }

    public Item getSelectedTarget() {
        Item item = BuiltInRegistries.ITEM.byId(targetId.get());
        return item == Items.AIR ? null : item;
    }

    public void setSelectedTarget(Item item) {
        if (isSpinning() || item == null) return;
        if (!player.level().isClientSide() && !ItemValueCache.hasValue(item)) return;
        targetId.set(BuiltInRegistries.ITEM.getId(item));
        setTargetCount(1);
    }

    public int getTargetCount() {
        return targetCount.get();
    }

    public int getMaxTargetCount() {
        Item target = getSelectedTarget();
        return target == null ? 1 : Math.min(64, new ItemStack(target).getMaxStackSize());
    }

    public void setTargetCount(int count) {
        if (!isSpinning()) targetCount.set(Math.clamp(count, 1, getMaxTargetCount()));
    }

    public boolean isSpinning() {
        return System.currentTimeMillis() < spinEndsAt;
    }

    public ItemStack getInputStack() {
        return input.getItem(0);
    }

    public SpinResult attemptSpin() {
        if (!(player instanceof ServerPlayer) || isSpinning() || !CooldownTracker.canSpin(player.getUUID())) return null;
        Item target = getSelectedTarget();
        if (target == null || !ItemValueCache.hasValue(target)) return null;
        double wager = ItemValueCache.getValue(getInputStack());
        double rewardValue = ItemValueCache.getBaseValue(target) * getTargetCount();
        if (wager <= 0 || rewardValue <= 0) return null;

        finishReward();
        SpinResult result = SpinResult.roll(UpgradeCalculator.successChance(wager, rewardValue), new Random());
        input.setItem(0, ItemStack.EMPTY);
        pendingReward = result.success() ? new ItemStack(target, getTargetCount()) : ItemStack.EMPTY;
        spinEndsAt = System.currentTimeMillis() + SpinResult.DURATION_MS;
        CooldownTracker.recordSpin(player.getUUID());
        broadcastChanges();
        return result;
    }

    @Override
    public void broadcastChanges() {
        if (!player.level().isClientSide() && !isSpinning()) finishReward();
        super.broadcastChanges();
    }

    private void finishReward() {
        if (pendingReward.isEmpty()) return;
        ItemStack reward = pendingReward;
        pendingReward = ItemStack.EMPTY;
        giveOrDrop(reward);
    }

    private void giveOrDrop(ItemStack stack) {
        if (!player.getInventory().add(stack)) player.drop(stack, false);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= slots.size() || isSpinning()) return ItemStack.EMPTY;
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (slotIndex == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            finishReward();
            ItemStack remaining = input.removeItemNoUpdate(0);
            if (!remaining.isEmpty()) giveOrDrop(remaining);
        }
    }
}
