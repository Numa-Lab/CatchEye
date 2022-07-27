package com.github.bun133.catcheye;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.command.arguments.EntityAnchorArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
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
    static final String MODID = "catcheye";
    static final String JOIN_MAGIC = "joined!";

    // Directly reference a log4j logger.
    static final Logger LOGGER = LogManager.getLogger();

    public CatchEye() {
        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
        registerNetwork();
    }

    public static String target_name = "";
    public static Vector3d nowLocation = null;
    public static Vector3d lastLocation = null;
    private static Long lastTime = null;
    public static final double one_tick_length_in_mill = 1000.0 / 20.0;
    public static final double update_interval_in_tick = 10.0;

    public static void pushLocation(Vector3d newLocation) {
        lastLocation = nowLocation;
        nowLocation = newLocation;
        lastTime = Util.milliTime();
    }

    public static int getDeltaTick() {
        if (lastTime == null) {
            return 0; // 初期化前は0.0ということで
        }
        return (int) Math.ceil((Util.milliTime() - lastTime) / one_tick_length_in_mill);
    }

    public static double getPartial() {
        if (lastTime == null) {
            return 0.0; // 初期化前は0.0ということで
        }
        double d = (Util.milliTime() - lastTime) / one_tick_length_in_mill;
        int i = (int) Math.ceil(d);
        return Math.max(Math.min(d - (double) i, 1.0), 0.0);
    }

    public static Vector3d getLerpVec(Vector3d from, Vector3d to, double pst) {
        return new Vector3d(
                from.x + (to.x - from.x) * pst,
                from.y + (to.y - from.y) * pst,
                from.z + (to.z - from.z) * pst
        );
    }

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
            if (nowLocation != null && lastLocation != null) {
                vanillaLookAt(getLerpVec(lastLocation, nowLocation, getDeltaTick() / update_interval_in_tick), getLerpVec(lastLocation, nowLocation, (getDeltaTick() + 1) / update_interval_in_tick), clientPlayer, getPartial());
            }
            return;
        }

        PlayerEntity real_target = target.get();
        pushLocation(new Vector3d(real_target.lastTickPosX, real_target.lastTickPosY, real_target.lastTickPosZ));
        lookAt(nowLocation, real_target.getPositionVec(), clientPlayer, partialTicks);
    }

    private void vanillaLookAt(Vector3d from, Vector3d to, PlayerEntity player, double partialTick) {
        Vector3d fromPoint = player.getPositionVec().add(0, player.getEyeHeight(), 0);
        vanillaLookAt(
                fromPoint,
                new Vector3d(
                        MathHelper.lerp(partialTick, from.x, to.x),
                        MathHelper.lerp(partialTick, from.y, to.y),
                        MathHelper.lerp(partialTick, from.z, to.z)
                ),
                player
        );
    }

    private void vanillaLookAt(Vector3d from, Vector3d to, PlayerEntity player) {
        double d0 = to.x - from.x;
        double d1 = to.y - from.y;
        double d2 = to.z - from.z;
        double d3 = MathHelper.sqrt(d0 * d0 + d2 * d2);
        player.rotationPitch = MathHelper.wrapDegrees((float) (-(MathHelper.atan2(d1, d3) * (double) (180F / (float) Math.PI))));
        player.rotationYaw = MathHelper.wrapDegrees((float) (MathHelper.atan2(d2, d0) * (double) (180F / (float) Math.PI)) - 90.0F);
        player.setRotationYawHead(player.rotationYaw);
        player.prevRotationPitch = player.rotationPitch;
        player.prevRotationYaw = player.rotationYaw;
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

    public static void handle(UpdatePotision message, Supplier<NetworkEvent.Context> ctx) {
        if (message == null || message.getTarget() == null)
            return;

        CatchEye.target_name = message.getTarget();
        Vector3d position = message.getPosition();
        if (position != null && !target_name.isEmpty()) {
            pushLocation(position);
        }
        CatchEye.LOGGER.info("Update target: " + message.getTarget());
        ctx.get().setPacketHandled(true);
    }

    @SubscribeEvent
    public void onJoin(ClientPlayerNetworkEvent.LoggedInEvent e) {
        sendJoinPacket();
    }

    public static void sendJoinPacket() {
        PacketDispatcher.channel.sendToServer(new HelloPacket(JOIN_MAGIC));
    }
}
