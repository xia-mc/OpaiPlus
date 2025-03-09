package asia.lira.opaiplus.internal;

import asia.lira.opaiplus.OpaiPlus;
import asia.lira.opaiplus.internal.unsafe.DeobfuscateUtils;
import asia.lira.opaiplus.utils.ReflectionUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import today.opai.api.dataset.BlockPosition;
import today.opai.api.enums.EnumDirection;
import today.opai.api.enums.EnumEntityAction;
import today.opai.api.events.EventPacketSend;
import today.opai.api.interfaces.EventHandler;
import today.opai.api.interfaces.game.item.ItemStack;
import today.opai.api.interfaces.game.network.client.CPacket0BEntityAction;

/*
这不是真的NetworkManager类的映射。
 */
public class NetworkManager implements EventHandler {
    private static CPacket0BEntityAction START_SPRINTING = null;
    private static CPacket0BEntityAction STOP_SPRINTING = null;

    private NetworkManager() {
    }

    public static void sendPacket(Object packet) {
        Object object = ReflectionUtils.getDeclared(ReflectionUtils.getClass("MatrixShield.iR"), "b");
        object = ReflectionUtils.call(object, "f");
        object = ReflectionUtils.call(object, "d");
        ReflectionUtils.call(object, "a",
                new Class[]{ReflectionUtils.getClass("MatrixShield.TX")},
                new Object[]{packet}
        );
    }

    public static void sendPacketNoEvent(Object packet) {
        Object object = ReflectionUtils.getDeclared(ReflectionUtils.getClass("MatrixShield.iR"), "b");
        object = ReflectionUtils.call(object, "f");
        object = ReflectionUtils.call(object, "d");
        ReflectionUtils.call(object, "b",
                new Class[]{ReflectionUtils.getClass("MatrixShield.TX")},
                new Object[]{packet}
        );
    }

    public static @NotNull Object createC08(@NotNull BlockPosition blockPos, int direction,
                                            @Nullable ItemStack itemStack, float hitPosX, float hitPosY, float hitPosZ) {
        Object mcBlockPos = ReflectionUtils.callConstructor(ReflectionUtils.getClass("MatrixShield.Zs"),
                new Class[]{int.class, int.class, int.class},
                new Object[]{blockPos.x, blockPos.y, blockPos.z}
        );
        Object mcItemStack = itemStack == null ? null : ReflectionUtils.get(itemStack, "h");

        return ReflectionUtils.callConstructor(ReflectionUtils.getClass("MatrixShield.UG"),
                new Class[]{mcBlockPos.getClass(), int.class, ReflectionUtils.getClass("MatrixShield.Sp"),
                        float.class, float.class, float.class},
                new Object[]{mcBlockPos, direction, mcItemStack,
                        hitPosX, hitPosY, hitPosZ}
        );
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
