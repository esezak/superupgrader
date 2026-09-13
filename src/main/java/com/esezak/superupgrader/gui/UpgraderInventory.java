package com.esezak.superupgrader.gui;

import net.minecraft.world.SimpleContainer;

/**
 * A simple 1-slot inventory for the upgrader input.
 */
public class UpgraderInventory extends SimpleContainer {

    public UpgraderInventory() {
        super(1);
    }

    @Override
    public int getMaxStackSize() {
        return 64;
    }
}
