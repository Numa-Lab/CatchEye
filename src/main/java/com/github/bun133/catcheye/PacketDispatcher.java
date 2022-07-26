package com.github.bun133.catcheye;

import com.google.common.base.Suppliers;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;

import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class PacketDispatcher {

    public static final String PROTOCOL_VERSION = "CE01";
    public static final Predicate<String> VANILLA_OR_HANDSHAKE =
            ((Predicate<String>) NetworkRegistry.ACCEPTVANILLA::equals).or(PROTOCOL_VERSION::equals);

    public static final SimpleChannel channel = NetworkRegistry.ChannelBuilder
            .named(new ResourceLocation(CatchEye.MODID, "position"))
            .clientAcceptedVersions(VANILLA_OR_HANDSHAKE)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .simpleChannel();

    private static final Supplier<Void> register = Suppliers.memoize(() -> {
        PacketDispatcher.channel.registerMessage(
                0,
                UpdatePotision.class,
                UpdatePotision::encode,
                UpdatePotision::decode,
                PacketDispatcher::handleUpdate
        );

        PacketDispatcher.channel.registerMessage(
                1,
                HelloPacket.class,
                HelloPacket::encode,
                HelloPacket::decode,
                PacketDispatcher::handleHello
        );
        return null;
    });

    private static BiConsumer<UpdatePotision, Supplier<NetworkEvent.Context>> clientHandler;
    private static BiConsumer<UpdatePotision, Supplier<NetworkEvent.Context>> serverHandler;

    public static void registerClient(BiConsumer<UpdatePotision, Supplier<NetworkEvent.Context>> handler) {
        register.get();
        clientHandler = handler;
    }

    public static void registerServer(BiConsumer<UpdatePotision, Supplier<NetworkEvent.Context>> handler) {
        register.get();
        serverHandler = handler;
    }

    private static void handleUpdate(UpdatePotision message, Supplier<NetworkEvent.Context> ctx) {
        if (ctx.get().getDirection().getReceptionSide().isClient()) {
            if (clientHandler != null)
                clientHandler.accept(message, ctx);
        } else {
            if (serverHandler != null)
                serverHandler.accept(message, ctx);
        }
    }

    private static void handleHello(HelloPacket message, Supplier<NetworkEvent.Context> ctx) {
        // Not Reachable
    }
}