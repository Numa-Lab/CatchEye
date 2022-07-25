package com.github.bun133.catcheye;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.command.arguments.EntityAnchorArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;
import java.util.function.Supplier;

// The value here should match an entry in the META-INF/mods.toml file
@Mod("catcheye")
public class CatchEye {
    static String MODID = "catcheye";

    // Directly reference a log4j logger.
    static final Logger LOGGER = LogManager.getLogger();

    public CatchEye() {
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        registerNetwork();
    }

    public static String target_name = "";
    public static Vector3d lastLocation = null;

    @SubscribeEvent
    public void onTick(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;

        Minecraft instance = Minecraft.getInstance();
        if (instance == null) {
            return;
        }

        ClientPlayerEntity clientPlayer = instance.player;
        if (clientPlayer == null) {
            return;
        }

        double partialTicks = e.renderTickTime;
        Optional<? extends PlayerEntity> target = clientPlayer.world.getPlayers().stream().filter((p) -> p.getGameProfile().getName().equals(CatchEye.target_name)).findFirst();
        if (!target.isPresent()) {
            if (target_name.isEmpty()) return;

            if (lastLocation != null) {
                lookAt(lastLocation, lastLocation, clientPlayer, partialTicks);
            }
            return;
        }

        PlayerEntity real_target = target.get();
        lastLocation = new Vector3d(real_target.lastTickPosX, real_target.lastTickPosY, real_target.lastTickPosZ);
        lookAt(lastLocation, real_target.getPositionVec(), clientPlayer, partialTicks);
    }

    private void lookAt(Vector3d lastTickPos, Vector3d nowPos, ClientPlayerEntity clientPlayer, double partialTicks) {
        double deltaMeX = clientPlayer.getPosX() - clientPlayer.lastTickPosX;
        double deltaMeY = clientPlayer.getPosY() - clientPlayer.lastTickPosY;
        double deltaMeZ = clientPlayer.getPosZ() - clientPlayer.lastTickPosZ;

        double youX = MathHelper.lerp(partialTicks, lastTickPos.getX() + deltaMeX, nowPos.getX());
        double youY = MathHelper.lerp(partialTicks, lastTickPos.getY() + deltaMeY, nowPos.getY());
        double youZ = MathHelper.lerp(partialTicks, lastTickPos.getZ() + deltaMeZ, nowPos.getZ());

        clientPlayer.lookAt(EntityAnchorArgument.Type.FEET, new Vector3d(youX, youY, youZ));
    }

    private static void registerNetwork() {
        PacketDispatcher.registerClient(CatchEye::handle);
    }

    public static void handle(PacketContainer message, Supplier<NetworkEvent.Context> ctx) {
        if (message == null || message.getTarget() == null)
            return;

        CatchEye.target_name = message.getTarget();
        Vector3d position = message.getPosition();
        if (position != null && !target_name.isEmpty()) {
            CatchEye.lastLocation = position;
        }
        CatchEye.LOGGER.info("Update target: " + message.getTarget());
        ctx.get().setPacketHandled(true);
    }
}
