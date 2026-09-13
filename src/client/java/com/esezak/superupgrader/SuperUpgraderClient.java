package com.esezak.superupgrader;

import com.esezak.superupgrader.gui.UpgraderScreen;
import com.esezak.superupgrader.gui.UpgraderScreenHandler;
import com.esezak.superupgrader.network.ModNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;

public class SuperUpgraderClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Bind screen handler to screen
        MenuScreens.register(UpgraderScreenHandler.SCREEN_HANDLER_TYPE, UpgraderScreen::new);

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.ValidTargetsS2CPayload.TYPE, (payload, context) ->
                context.client().execute(() -> com.esezak.superupgrader.gui.ClientCatalog.replace(payload.targets())));
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                com.esezak.superupgrader.gui.ClientCatalog.replace(java.util.List.of()));

        // Register client networking handlers
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.SpinResultS2CPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.gui.screen() instanceof UpgraderScreen screen) {
                    screen.onSpinResult(payload.result());
                }
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.CooldownS2CPayload.TYPE, (payload, context) -> {
            context.client().execute(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.gui.screen() instanceof UpgraderScreen screen) {
                    screen.setCooldownRemaining(payload.cooldownRemainingMs());
                }
            });
        });
    }
}
