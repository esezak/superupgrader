package com.esezak.superupgrader.value;

import com.esezak.superupgrader.SuperUpgrader;
import com.esezak.superupgrader.config.ModConfig;
import it.unimi.dsi.fastutil.objects.Object2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Caches computed values for all items. Rebuilt once on server start.
 */
public class ItemValueCache {
    private static final Object2DoubleMap<Item> VALUES = new Object2DoubleLinkedOpenHashMap<>();
    private static List<Item> validTargets = Collections.emptyList();

    public static void rebuild(MinecraftServer server) {
        VALUES.clear();

        // Step 1: Walk all recipes to compute base values
        RecipeGraphWalker walker = new RecipeGraphWalker(server);
        Object2DoubleMap<Item> computed = walker.computeAll();
        VALUES.putAll(computed);

        // Step 2: Apply config overrides
        ModConfig config = ModConfig.get();
        for (var entry : config.valueOverrides.entrySet()) {
            Identifier id = Identifier.tryParse(entry.getKey());
            if (id == null || entry.getValue() == null || !Double.isFinite(entry.getValue()) || entry.getValue() <= 0) continue;
            Item item = BuiltInRegistries.ITEM.getValue(id);
            if (item != null && item != Items.AIR) {
                VALUES.put(item, entry.getValue().doubleValue());
            }
        }

        // Step 3: Apply multipliers
        for (var entry : VALUES.object2DoubleEntrySet()) {
            Item item = entry.getKey();
            double value = entry.getDoubleValue();

            // Per-item multiplier
            Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
            if (config.valueMultipliers.containsKey(itemId.toString())) {
                Double multiplier = config.valueMultipliers.get(itemId.toString());
                if (multiplier != null && Double.isFinite(multiplier) && multiplier > 0) value *= multiplier;
            }

            // Global multiplier
            value *= config.globalValueMultiplier;

            entry.setValue(value);
        }

        // Step 4: Build valid targets list (exclude filtered items)
        List<Item> targets = new ArrayList<>();
        for (var entry : VALUES.object2DoubleEntrySet()) {
            Item item = entry.getKey();
            if (Double.isFinite(entry.getDoubleValue()) && entry.getDoubleValue() > 0 && !ItemExclusionFilter.isExcluded(item)) {
                targets.add(item);
            }
        }
        // Sort by value ascending for the target list
        targets.sort(java.util.Comparator.comparingDouble((Item item) -> VALUES.getDouble(item))
                .thenComparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
        validTargets = Collections.unmodifiableList(targets);

        SuperUpgrader.LOGGER.info("Value cache: {} items priced, {} valid targets.",
                VALUES.size(), validTargets.size());
    }

    /**
     * Returns the value of a stack, accounting for durability and quantity.
     */
    public static double getValue(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        double baseValue = getBaseValue(stack.getItem());
        if (baseValue <= 0) return 0;

        double value = baseValue * stack.getCount();
        value = DurabilityAdjuster.adjust(value, stack);
        return value;
    }

    /**
     * Returns the base value of an item (1 unit, full durability).
     */
    public static double getBaseValue(Item item) {
        double value = VALUES.getOrDefault(item, 0.0);
        return Double.isFinite(value) && value > 0 && !ItemExclusionFilter.isExcluded(item) ? value : 0;
    }

    public static boolean hasValue(Item item) {
        return getBaseValue(item) > 0;
    }

    public static List<Item> getValidTargets() {
        return validTargets;
    }

    public static int size() {
        return VALUES.size();
    }
}
