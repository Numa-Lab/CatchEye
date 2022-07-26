package com.github.bun133.catcheye;

import net.minecraft.network.PacketBuffer;

public class HelloPacket {
    final String key;

    HelloPacket(String key) {
        this.key = key;
    }

    public static void encode(HelloPacket message, PacketBuffer buffer) {
        buffer.writeString(DataSerializer.encode(message));
    }

    public static HelloPacket decode(PacketBuffer buffer) {
        String json = buffer.readString();
        HelloPacket data = DataSerializer.decode(json, HelloPacket.class);
        if (data == null) {
            CatchEye.LOGGER.warn("Failed to decode packet");
        }
        return data;
    }
}
