package com.esezak.superupgrader.network;

import com.esezak.superupgrader.SuperUpgrader;
import com.esezak.superupgrader.gui.UpgraderScreenHandler;
import com.esezak.superupgrader.logic.CooldownTracker;
import com.esezak.superupgrader.logic.SpinResult;
import com.esezak.superupgrader.value.ItemValueCache;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class ModNetworking {

    // ═══════════════════════════════════════════════════
    // Payload Types
    // ═══════════════════════════════════════════════════

    // C2S: Player selected a target item
    public record SelectTargetC2SPayload(Identifier targetId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SelectTargetC2SPayload> TYPE =
                new CustomPacketPayload.Type<>(SuperUpgrader.id("select_target"));
        public static final StreamCodec<FriendlyByteBuf, SelectTargetC2SPayload> CODEC =
                StreamCodec.of(
                        (buf, payload) -> buf.writeIdentifier(payload.targetId),
                        buf -> new SelectTargetC2SPayload(buf.readIdentifier())
                );

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // C2S: Number of target items requested
    public record SetTargetCountC2SPayload(int count) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SetTargetCountC2SPayload> TYPE =
                new CustomPacketPayload.Type<>(SuperUpgrader.id("set_target_count"));
        public static final StreamCodec<FriendlyByteBuf, SetTargetCountC2SPayload> CODEC =
                StreamCodec.of(
                        (buf, payload) -> buf.writeVarInt(payload.count),
                        buf -> new SetTargetCountC2SPayload(buf.readVarInt())
                );

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // C2S: Player clicked UPGRADE
    public record RequestSpinC2SPayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RequestSpinC2SPayload> TYPE =
                new CustomPacketPayload.Type<>(SuperUpgrader.id("request_spin"));
        public static final StreamCodec<FriendlyByteBuf, RequestSpinC2SPayload> CODEC =
                StreamCodec.of(
                        (buf, payload) -> {},
                        buf -> new RequestSpinC2SPayload()
                );

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // S2C: Spin result for animation
    public record SpinResultS2CPayload(SpinResult result) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SpinResultS2CPayload> TYPE =
                new CustomPacketPayload.Type<>(SuperUpgrader.id("spin_result"));
        public static final StreamCodec<FriendlyByteBuf, SpinResultS2CPayload> CODEC =
                StreamCodec.of(
                        (buf, payload) -> payload.result.write(buf),
                        buf -> new SpinResultS2CPayload(SpinResult.read(buf))
                );

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // S2C: Valid targets list
    public record ValidTargetsS2CPayload(List<TargetEntry> targets) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ValidTargetsS2CPayload> TYPE =
                new CustomPacketPayload.Type<>(SuperUpgrader.id("valid_targets"));
        public static final StreamCodec<FriendlyByteBuf, ValidTargetsS2CPayload> CODEC =
                StreamCodec.of(
                        (buf, payload) -> {
                            buf.writeVarInt(payload.targets.size());
                            for (TargetEntry entry : payload.targets) {
                                buf.writeIdentifier(entry.itemId);
                                buf.writeDouble(entry.value);
                            }
                        },
                        buf -> {
                            int size = buf.readVarInt();
                            if (size < 0 || size > 65536) throw new IllegalArgumentException("Invalid catalog size");
                            List<TargetEntry> list = new ArrayList<>(size);
                            for (int i = 0; i < size; i++) {
                                list.add(new TargetEntry(buf.readIdentifier(), buf.readDouble()));
                            }
                            return new ValidTargetsS2CPayload(list);
                        }
                );

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record TargetEntry(Identifier itemId, double value) {}

    // S2C: Cooldown info
    public record CooldownS2CPayload(long cooldownRemainingMs) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<CooldownS2CPayload> TYPE =
                new CustomPacketPayload.Type<>(SuperUpgrader.id("cooldown"));
        public static final StreamCodec<FriendlyByteBuf, CooldownS2CPayload> CODEC =
                StreamCodec.of(
                        (buf, payload) -> buf.writeLong(payload.cooldownRemainingMs),
                        buf -> new CooldownS2CPayload(buf.readLong())
                );

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    // ═══════════════════════════════════════════════════
    // Registration
    // ═══════════════════════════════════════════════════

    public static void registerServer() {
        // Register payload types
        PayloadTypeRegistry.serverboundPlay().register(SelectTargetC2SPayload.TYPE, SelectTargetC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(SetTargetCountC2SPayload.TYPE, SetTargetCountC2SPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(RequestSpinC2SPayload.TYPE, RequestSpinC2SPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(SpinResultS2CPayload.TYPE, SpinResultS2CPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ValidTargetsS2CPayload.TYPE, ValidTargetsS2CPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(CooldownS2CPayload.TYPE, CooldownS2CPayload.CODEC);

        // Register server-side handlers
        ServerPlayNetworking.registerGlobalReceiver(SelectTargetC2SPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                if (player.containerMenu instanceof UpgraderScreenHandler handler) {
                    Item item = BuiltInRegistries.ITEM.getValue(payload.targetId());
                    if (item != null && item != Items.AIR && ItemValueCache.hasValue(item)) {
                        handler.setSelectedTarget(item);
                    }
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(SetTargetCountC2SPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                if (player.containerMenu instanceof UpgraderScreenHandler handler) {
                    handler.setTargetCount(payload.count());
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(RequestSpinC2SPayload.TYPE, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayer player = context.player();
                if (player.containerMenu instanceof UpgraderScreenHandler handler) {
                    // Check cooldown
                    if (!CooldownTracker.canSpin(player.getUUID())) {
                        long remaining = CooldownTracker.getRemainingCooldownMs(player.getUUID());
                        ServerPlayNetworking.send(player, new CooldownS2CPayload(remaining));
                        return;
                    }

                    SpinResult result = handler.attemptSpin();
                    if (result != null) {
                        ServerPlayNetworking.send(player, new SpinResultS2CPayload(result));
                        sendCooldown(player);
                    }
                }
            });
        });
    }

    /**
     * Send the valid targets list to a player.
     */
    public static void sendValidTargets(ServerPlayer player) {
        List<Item> targets = ItemValueCache.getValidTargets();
        List<TargetEntry> entries = new ArrayList<>(targets.size());
        for (Item item : targets) {
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            double value = ItemValueCache.getBaseValue(item);
            entries.add(new TargetEntry(id, value));
        }
        ServerPlayNetworking.send(player, new ValidTargetsS2CPayload(entries));
    }

    public static void sendCooldown(ServerPlayer player) {
        ServerPlayNetworking.send(player, new CooldownS2CPayload(CooldownTracker.getRemainingCooldownMs(player.getUUID())));
    }

    // Client-side registration (called from client entrypoint)
    public static void registerClient() {
        // Client handlers are registered in the client module
        // They will be set up in UpgraderScreen when it opens
    }
}
