package com.esezak.superupgrader.value;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import java.util.LinkedHashMap;
import java.util.Map;

/** Named material prices transcribed from the Upgrader 1.2.0 reference constants. */
public final class BaseValues {
    private BaseValues() {}

    public static Map<Item, Double> seeds() {
        Map<Item, Double> values = new LinkedHashMap<>();
        add(values, 1, Items.DIRT, Items.COBBLESTONE, Items.COBBLED_DEEPSLATE, Items.SAND,
                Items.RED_SAND, Items.GRAVEL, Items.NETHERRACK, Items.END_STONE, Items.GRANITE,
                Items.DIORITE, Items.ANDESITE, Items.TUFF, Items.CALCITE, Items.BASALT,
                Items.BLACKSTONE, Items.SNOWBALL, Items.WHEAT_SEEDS, Items.KELP);
        add(values, 2, Items.STONE, Items.DEEPSLATE, Items.FLINT, Items.BAMBOO, Items.CACTUS,
                Items.SUGAR_CANE, Items.OBSIDIAN, Items.SOUL_SAND, Items.SOUL_SOIL,
                Items.CLAY_BALL, Items.MOSS_BLOCK);
        add(values, 4, Items.OAK_LOG, Items.SPRUCE_LOG, Items.BIRCH_LOG, Items.JUNGLE_LOG,
                Items.ACACIA_LOG, Items.DARK_OAK_LOG, Items.MANGROVE_LOG, Items.CHERRY_LOG,
                Items.CRIMSON_STEM, Items.WARPED_STEM);
        add(values, 3, Items.WHEAT, Items.POTATO, Items.CARROT, Items.BEETROOT, Items.APPLE, Items.EGG);
        add(values, 6, Items.PORKCHOP, Items.BEEF, Items.CHICKEN, Items.MUTTON, Items.COD, Items.SALMON);
        add(values, 30, Items.HONEYCOMB, Items.PUFFERFISH, Items.CHORUS_FRUIT);
        add(values, 4, Items.STRING, Items.FEATHER, Items.BONE, Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.INK_SAC);
        add(values, 12, Items.LEATHER, Items.SLIME_BALL, Items.RABBIT_HIDE, Items.PRISMARINE_SHARD, Items.PRISMARINE_CRYSTALS);
        add(values, 20, Items.GUNPOWDER, Items.MAGMA_CREAM, Items.GLOW_INK_SAC);
        add(values, 50, Items.ENDER_PEARL, Items.BLAZE_ROD, Items.PHANTOM_MEMBRANE, Items.NAUTILUS_SHELL);
        add(values, 120, Items.GHAST_TEAR, Items.RABBIT_FOOT, Items.SHULKER_SHELL);
        add(values, 6, Items.COAL, Items.CHARCOAL);
        add(values, 8, Items.REDSTONE, Items.GLOWSTONE_DUST);
        add(values, 10, Items.LAPIS_LAZULI, Items.RAW_COPPER);
        add(values, 12, Items.COPPER_INGOT, Items.QUARTZ, Items.AMETHYST_SHARD);
        add(values, 35, Items.RAW_IRON);
        add(values, 40, Items.IRON_INGOT);
        add(values, 55, Items.RAW_GOLD);
        add(values, 60, Items.GOLD_INGOT);
        add(values, 140, Items.EMERALD);
        add(values, 400, Items.DIAMOND);
        add(values, 1200, Items.ANCIENT_DEBRIS, Items.NETHERITE_SCRAP);
        add(values, 300, Items.HEART_OF_THE_SEA, Items.ECHO_SHARD, Items.SPONGE);
        add(values, 600, Items.SADDLE, Items.NAME_TAG);
        add(values, 800, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        add(values, 1500, Items.TOTEM_OF_UNDYING, Items.TRIDENT);
        add(values, 2500, Items.ENCHANTED_GOLDEN_APPLE, Items.ELYTRA);
        add(values, 4000, Items.NETHER_STAR);
        add(values, 20000, Items.DRAGON_EGG);
        return values;
    }

    private static void add(Map<Item, Double> values, double value, Item... items) {
        for (Item item : items) values.put(item, value);
    }
}
