package com.esezak.superupgrader;

import com.esezak.superupgrader.config.ModConfig;
import com.esezak.superupgrader.gui.UpgraderScreenHandler;
import com.esezak.superupgrader.item.ModItems;
import com.esezak.superupgrader.network.ModNetworking;
import com.esezak.superupgrader.value.ItemValueCache;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SuperUpgrader implements ModInitializer {
    public static final String MOD_ID = "superupgrader";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Super Upgrader initializing...");

        // Load config
        ModConfig.load();

        // Register items
        ModItems.register();

        // Register screen handler
        UpgraderScreenHandler.register();

        // Register networking
        ModNetworking.registerServer();

        // Build item value cache when the server starts
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Building item value cache...");
            long start = System.currentTimeMillis();
            ItemValueCache.rebuild(server);
            long elapsed = System.currentTimeMillis() - start;
            LOGGER.info("Item value cache built in {}ms — {} items priced.",
                    elapsed, ItemValueCache.size());
        });

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            if (success) {
                ItemValueCache.rebuild(server);
                for (var player : server.getPlayerList().getPlayers()) ModNetworking.sendValidTargets(player);
            }
        });

        LOGGER.info("Super Upgrader initialized.");
    }
}
