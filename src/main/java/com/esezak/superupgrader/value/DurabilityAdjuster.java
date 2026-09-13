package com.esezak.superupgrader.value;

import net.minecraft.world.item.ItemStack;

/**
 * Adjusts item value based on remaining durability for damageable items.
 */
public class DurabilityAdjuster {

    /**
     * If the item is damageable and damaged, reduces the value proportionally.
     *
     * @param baseValue the full-durability value (already multiplied by count if applicable)
     * @param stack     the item stack
     * @return adjusted value
     */
    public static double adjust(double baseValue, ItemStack stack) {
        if (!stack.isDamageableItem()) {
            return baseValue;
        }

        int maxDurability = stack.getMaxDamage();
        if (maxDurability <= 0) {
            return baseValue;
        }

        int remaining = maxDurability - stack.getDamageValue();
        double ratio = (double) remaining / maxDurability;
        return baseValue * Math.clamp(ratio, 0.05, 1.0);
    }
}
