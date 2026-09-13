package com.esezak.superupgrader.gui;

import com.esezak.superupgrader.network.ModNetworking;
import com.esezak.superupgrader.value.DurabilityAdjuster;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Client-only snapshot of the connected server's prices, including its configuration. */
public final class ClientCatalog {
    private static final Map<Item, Double> VALUES = new LinkedHashMap<>();
    private static int revision;

    private ClientCatalog() {}

    public static void replace(List<ModNetworking.TargetEntry> entries) {
        VALUES.clear();
        for (var entry : entries) {
            Item item = BuiltInRegistries.ITEM.getValue(entry.itemId());
            if (item != null && item != Items.AIR && Double.isFinite(entry.value()) && entry.value() > 0) {
                VALUES.put(item, entry.value());
            }
        }
        revision++;
    }

    public static int revision() { return revision; }
    public static List<Item> items() { return List.copyOf(VALUES.keySet()); }
    public static double value(Item item) { return VALUES.getOrDefault(item, 0.0); }
    public static double value(ItemStack stack) {
        return stack.isEmpty() ? 0 : DurabilityAdjuster.adjust(value(stack.getItem()), stack) * stack.getCount();
    }
}
