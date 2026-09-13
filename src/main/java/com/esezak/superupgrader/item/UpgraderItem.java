package com.esezak.superupgrader.item;

import com.esezak.superupgrader.gui.UpgraderScreenHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class UpgraderItem extends Item {

    public UpgraderItem(Properties properties) {
        super(properties);
    }

    @Override
    public @NotNull InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            com.esezak.superupgrader.network.ModNetworking.sendValidTargets(serverPlayer);
            serverPlayer.openMenu(new MenuProvider() {
                @Override
                public @NotNull Component getDisplayName() {
                    return Component.translatable("gui.superupgrader.title");
                }

                @Override
                public @NotNull AbstractContainerMenu createMenu(int syncId, Inventory playerInventory, Player player) {
                    return new UpgraderScreenHandler(syncId, playerInventory);
                }
            });
            com.esezak.superupgrader.network.ModNetworking.sendCooldown(serverPlayer);
        }

        return InteractionResult.SUCCESS;
    }
}
