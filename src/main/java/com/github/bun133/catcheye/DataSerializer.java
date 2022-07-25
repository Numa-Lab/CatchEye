package com.github.bun133.catcheye;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.util.math.vector.Vector3d;

import javax.annotation.Nullable;

public class DataSerializer {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Vector3d.class, new Vec3dTypeAdaptor())
            .create();

    public static String encode(Object object) {
        return gson.toJson(object);
    }

    @Nullable
    public static <T> T decode(String json, Class<T> classOfT) {
        try {
            return gson.fromJson(json, classOfT);
        } catch (Exception e) {
            return null;
        }
    }
}