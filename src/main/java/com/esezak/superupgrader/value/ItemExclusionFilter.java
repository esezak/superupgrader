package com.esezak.superupgrader.value;

import com.esezak.superupgrader.config.ModConfig;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;

import java.util.Set;

/**
 * Determines which items are excluded from the upgrader wheel.
 * These items cannot be used as input or appear as targets.
 */
public class ItemExclusionFilter {

    // Item classes to always exclude
    private static final Set<Class<?>> EXCLUDED_CLASSES = Set.of(
            SpawnEggItem.class);

    // Specific items to always exclude (by registry ID)
    private static final Set<String> EXCLUDED_ITEMS = Set.of(
            "minecraft:potion",
            "minecraft:splash_potion",
            "minecraft:lingering_potion",
            "minecraft:tipped_arrow",
            "minecraft:enchanted_book",
            "minecraft:suspicious_stew",
            "minecraft:knowledge_book",
            "minecraft:written_book",
            "minecraft:filled_map",
            "minecraft:bedrock",
            "minecraft:spawner",
            "minecraft:budding_amethyst",
            "minecraft:end_portal_frame",
            "minecraft:reinforced_deepslate",
            "minecraft:farmland",
            "minecraft:dirt_path",
            "minecraft:infested_stone",
            "minecraft:infested_cobblestone",
            "minecraft:infested_deepslate",
            "minecraft:infested_stone_bricks",
            "minecraft:infested_mossy_stone_bricks",
            "minecraft:infested_cracked_stone_bricks",
            "minecraft:infested_chiseled_stone_bricks",
            "minecraft:air",
            "minecraft:debug_stick",
            "minecraft:command_block",
            "minecraft:chain_command_block",
            "minecraft:repeating_command_block",
            "minecraft:command_block_minecart",
            "minecraft:structure_block",
            "minecraft:structure_void",
            "minecraft:jigsaw",
            "minecraft:barrier",
            "minecraft:light",
            "minecraft:petrified_oak_slab",
            "minecraft:test_block",
            "minecraft:test_instance_block");

    public static boolean isExcluded(Item item) {
        if (item == Items.AIR)
            return true;

        // Check class hierarchy (spawn eggs, etc.)
        for (Class<?> cls : EXCLUDED_CLASSES) {
            if (cls.isInstance(item))
                return true;
        }

        // Check specific items
        Identifier id = BuiltInRegistries.ITEM.getKey(item);
        if (id.getNamespace().equals("superupgrader") || id.getNamespace().equals("upgrader"))
            return true;
        if (id != null && EXCLUDED_ITEMS.contains(id.toString()))
            return true;

        // Check config blacklist
        if (id != null && ModConfig.get().blacklist.contains(id.toString()))
            return true;

        return false;
    }
}
