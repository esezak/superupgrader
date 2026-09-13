package com.esezak.superupgrader.item;

import com.esezak.superupgrader.SuperUpgrader;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public class ModItems {
    public static final ResourceKey<Item> UPGRADER_KEY = ResourceKey.create(
            Registries.ITEM,
            SuperUpgrader.id("upgrader")
    );

    public static final UpgraderItem UPGRADER = new UpgraderItem(new Item.Properties()
            .setId(UPGRADER_KEY)
            .stacksTo(1));

    public static void register() {
        Registry.register(BuiltInRegistries.ITEM, UPGRADER_KEY, UPGRADER);

        // Add to the Tools & Utilities creative tab
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(output -> output.accept(UPGRADER));
    }
}
