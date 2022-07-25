package com.github.bun133.catcheye;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.vector.Vector3d;


public class PacketContainer {
    private final String target;
    private final Vector3d position;

    public PacketContainer(String target, Vector3d position) {
        this.target = target;
        this.position = position;
    }

    public String getTarget() {
        return target;
    }

    public Vector3d getPosition() {
        return position;
    }

    public static void encode(PacketContainer message, PacketBuffer buffer) {
        buffer.writeString(DataSerializer.encode(message));
    }

    public static PacketContainer decode(PacketBuffer buffer) {
        String json = buffer.readString();
        PacketContainer data = DataSerializer.decode(json, PacketContainer.class);
        if (data == null) {
            CatchEye.LOGGER.warn("Failed to decode packet");
        }
        return data;
    }
}