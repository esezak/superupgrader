package com.esezak.superupgrader.value;

import com.esezak.superupgrader.config.ModConfig;
import it.unimi.dsi.fastutil.objects.Object2DoubleLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.item.crafting.display.SlotDisplayContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Resolves material costs before assigning rarity fallbacks, as in the reference economy. */
public final class RecipeGraphWalker {
    private static final int MAX_PASSES = 16;
    private final MinecraftServer server;

    public RecipeGraphWalker(MinecraftServer server) {
        this.server = server;
    }

    public Object2DoubleMap<Item> computeAll() {
        Object2DoubleMap<Item> values = new Object2DoubleLinkedOpenHashMap<>();
        values.putAll(BaseValues.seeds());
        for (Item item : BuiltInRegistries.ITEM) {
            Double override = ModConfig.get().valueOverrides.get(BuiltInRegistries.ITEM.getKey(item).toString());
            if (override != null && Double.isFinite(override) && override > 0) values.put(item, override.doubleValue());
        }
        Set<Item> fixed = new HashSet<>(values.keySet());
        List<PricedRecipe> recipes = normalizeRecipes();
        for (int pass = 0; pass < MAX_PASSES; pass++) {
            boolean changed = false;
            for (PricedRecipe recipe : recipes) {
                if (fixed.contains(recipe.output())) continue;
                double cost = recipe.cost(values);
                if (Double.isFinite(cost) && (!values.containsKey(recipe.output())
                        || cost < values.getDouble(recipe.output()) - 0.000001)) {
                    values.put(recipe.output(), cost);
                    changed = true;
                }
            }
            if (!changed) break;
        }
        // Fallbacks must never become cheap ingredients in cycles or smithing recipes.
        for (Item item : BuiltInRegistries.ITEM) {
            if (!values.containsKey(item) && !ItemExclusionFilter.isExcluded(item)) {
                values.put(item, switch (new ItemStack(item).getRarity()) {
                    case COMMON -> 25.0;
                    case UNCOMMON -> 150.0;
                    case RARE -> 600.0;
                    case EPIC -> 2500.0;
                });
            }
        }
        return values;
    }

    private List<PricedRecipe> normalizeRecipes() {
        List<PricedRecipe> result = new ArrayList<>();
        var context = SlotDisplayContext.fromLevel(server.overworld());
        for (var holder : server.getRecipeManager().getRecipes()) {
            Recipe<?> recipe = holder.value();
            if (recipe.isSpecial() || recipe instanceof SmithingTrimRecipe) continue;
            List<Ingredient> ingredients;
            if (recipe instanceof SmithingTransformRecipe smithing) {
                // Reference pricing counts equipment and addition, excluding the template.
                ingredients = new ArrayList<>();
                ingredients.add(smithing.baseIngredient());
                smithing.additionIngredient().ifPresent(ingredients::add);
            } else {
                var placement = recipe.placementInfo();
                if (placement.isImpossibleToPlace()) continue;
                ingredients = placement.ingredients();
            }
            if (ingredients.isEmpty()) continue;
            List<List<Item>> options = ingredients.stream()
                    .map(ingredient -> ingredient.items().map(Holder::value).toList()).toList();
            if (options.stream().anyMatch(List::isEmpty)) continue;
            for (var display : recipe.display()) {
                ItemStack output = display.result().resolveForFirstStack(context);
                if (!output.isEmpty()) result.add(new PricedRecipe(output.getItem(), output.getCount(), options));
            }
        }
        return result;
    }

    private record PricedRecipe(Item output, int count, List<List<Item>> ingredients) {
        double cost(Object2DoubleMap<Item> values) {
            double total = 0;
            for (List<Item> options : ingredients) {
                double cheapest = Double.POSITIVE_INFINITY;
                for (Item item : options) {
                    if (values.containsKey(item)) cheapest = Math.min(cheapest, values.getDouble(item));
                }
                total += cheapest;
            }
            return Math.max(1, total / count);
        }
    }
}
