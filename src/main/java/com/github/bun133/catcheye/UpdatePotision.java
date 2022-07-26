package com.github.bun133.catcheye;

import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.vector.Vector3d;


public class UpdatePotision {
    private final String target;
    private final Vector3d position;

    public UpdatePotision(String target, Vector3d position) {
        this.target = target;
        this.position = position;
    }

    public String getTarget() {
        return target;
    }

    public Vector3d getPosition() {
        return position;
    }

    public static void encode(UpdatePotision message, PacketBuffer buffer) {
        buffer.writeString(DataSerializer.encode(message));
    }

    public static UpdatePotision decode(PacketBuffer buffer) {
        String json = buffer.readString();
        UpdatePotision data = DataSerializer.decode(json, UpdatePotision.class);
        if (data == null) {
            CatchEye.LOGGER.warn("Failed to decode packet");
        }
        return data;
    }
}