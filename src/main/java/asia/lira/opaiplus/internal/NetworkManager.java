package asia.lira.opaiplus.internal;

import asia.lira.opaiplus.OpaiPlus;
import asia.lira.opaiplus.utils.ReflectionUtils;
import asia.lira.opaiplus.utils.StringUtils;
import org.jetbrains.annotations.NotNull;
import today.opai.api.enums.EnumEntityAction;
import today.opai.api.events.EventPacketSend;
import today.opai.api.interfaces.EventHandler;
import today.opai.api.interfaces.game.network.client.CPacket0BEntityAction;
import today.opai.api.interfaces.game.network.server.SPacket12Velocity;

/*
这不是真的NetworkManager类的映射。

一些辅助信息：
类 MatrixShield.SM 对应 NetworkManager
有方法protected void channelRead0(ChannelHandlerContext, Object)

MatrixShield.bw 为SPacket12Velocity实现类，构造传入MatrixShield.Uz
MatrixShield.Uz 对应S12EntityVelocityPacket，构造传入(int, double, double, double)

byd rename，根据一个opai dev的说法，这些信息在b18.4-beta后不再可靠
 */

public class NetworkManager implements EventHandler {
    private static CPacket0BEntityAction START_SPRINTING = null;
    private static CPacket0BEntityAction STOP_SPRINTING = null;

    private NetworkManager() {
    }

    public static @NotNull SPacket12Velocity createS12(int entityId, double motionX, double motionY, double motionZ) {
        try {
            Object packet = ReflectionUtils.callConstructor(
                    ReflectionUtils.getClass("MatrixShield.Uz"),
                    new Class<?>[]{int.class, double.class, double.class, double.class},
                    new Object[]{entityId, motionX, motionY, motionZ}
            );  // S12EntityVelocityPacket

            Object result = ReflectionUtils.callConstructor(
                    ReflectionUtils.getClass("MatrixShield.bw"),
                    packet
            );

            return (SPacket12Velocity) result;
        } catch (RuntimeException e) {
            OpaiPlus.error("Failed to create S12 Packet. Maybe Opai update?");
            OpaiPlus.error(StringUtils.limitLength(StringUtils.getStackTraceAsString(e), 512));
            throw new RuntimeException(e);
        }
    }

    public static CPacket0BEntityAction createStartSprint() {
        if (START_SPRINTING != null)
            return START_SPRINTING;

        OpaiPlus.error("Failed to create C0B Packet. Play more to finish initializing.");
        throw new RuntimeException();
    }

    public static CPacket0BEntityAction createStopSprint() {
        if (STOP_SPRINTING != null)
            return STOP_SPRINTING;

        OpaiPlus.error("Failed to create C0B Packet. Play more to finish initializing.");
        throw new RuntimeException();
    }

    public static void init() {
        OpaiPlus.getAPI().registerEvent(new NetworkManager());
    }

    @Override
    public void onPacketSend(@NotNull EventPacketSend event) {
        if (START_SPRINTING != null && STOP_SPRINTING != null) return;

        if (event.getPacket() instanceof CPacket0BEntityAction) {
            CPacket0BEntityAction packet = (CPacket0BEntityAction) event.getPacket();
            if (packet.getAction() == EnumEntityAction.START_SPRINTING) {
                if (START_SPRINTING == null) START_SPRINTING = packet;
            } else if (packet.getAction() == EnumEntityAction.STOP_SPRINTING) {
                if (STOP_SPRINTING == null) STOP_SPRINTING = packet;
            }
        }
    }
}
