package dev.yukiinotenshi.simplephonepromax.network;

import dev.yukiinotenshi.foxoperator.LocalOperator;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.LoggerFactory;

public final class FabricLocalOperator implements ModInitializer {
    private LocalOperator operator;

    @Override public void onInitialize() {
        PayloadTypeRegistry.serverboundPlay().register(OperatorPayload.TYPE, OperatorPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(OperatorPayload.TYPE, OperatorPayload.CODEC);
        if (FabricLocalOperator.class.getResource("/fox-operator/operator_stdio.py") == null) return;
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            var directory = server.getWorldPath(LevelResource.ROOT).resolve("fox-mobile-operator");
            var runtime = FabricLoader.getInstance().getConfigDir().resolve("fox-mobile-operator-runtime");
            var logger = LoggerFactory.getLogger("Fox Mobile Operator");
            operator = new LocalOperator(directory, runtime, System.getProperty("foxmobile.python", "auto"), logger::warn);
        });
        ServerPlayNetworking.registerGlobalReceiver(OperatorPayload.TYPE, (payload, context) -> {
            var player = context.player();
            if (operator != null && ServerPlayNetworking.canSend(player, OperatorPayload.TYPE)) {
                operator.receive(player, player.getUUID(), player.getGameProfile().name(), payload.data(),
                        context.server()::execute, data -> {
                            if (!player.isRemoved()) ServerPlayNetworking.send(player, new OperatorPayload(data));
                        });
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (operator != null) operator.disconnect(handler.player);
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (operator != null) operator.close();
            operator = null;
        });
    }
}
