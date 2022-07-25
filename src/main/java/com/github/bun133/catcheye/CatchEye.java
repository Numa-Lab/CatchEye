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

    public static String target = "";

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

        Optional<? extends PlayerEntity> target = clientPlayer.world.getPlayers().stream().filter((p) -> p.getGameProfile().getName().equals(CatchEye.target)).findFirst();
        if (!target.isPresent()) {
            return;
        }

        PlayerEntity real_target = target.get();

        double partialTicks = e.renderTickTime;

        double deltaMeX = clientPlayer.getPosX() - clientPlayer.lastTickPosX;
        double deltaMeY = clientPlayer.getPosY() - clientPlayer.lastTickPosY;
        double deltaMeZ = clientPlayer.getPosZ() - clientPlayer.lastTickPosZ;

        double youX = MathHelper.lerp(partialTicks, real_target.lastTickPosX + deltaMeX, real_target.getPosX());
        double youY = MathHelper.lerp(partialTicks, real_target.lastTickPosY + deltaMeY, real_target.getPosY());
        double youZ = MathHelper.lerp(partialTicks, real_target.lastTickPosZ + deltaMeZ, real_target.getPosZ());

        clientPlayer.lookAt(EntityAnchorArgument.Type.FEET, new Vector3d(youX, youY, youZ));
    }

    private static void registerNetwork() {
        PacketDispatcher.registerClient(CatchEye::handle);
    }

    public static void handle(PacketContainer message, Supplier<NetworkEvent.Context> ctx) {
        System.out.println("On Packet");
        if (message == null || message.getTarget() == null)
            return;

        CatchEye.target = message.getTarget();
        CatchEye.LOGGER.info("Update target: " + message.getTarget());
        ctx.get().setPacketHandled(true);
    }
}
