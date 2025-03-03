package asia.lira.opaiplus.modules.movement;

import asia.lira.opaiplus.internal.Module;
import asia.lira.opaiplus.utils.MathUtils;
import asia.lira.opaiplus.utils.MoveUtil;
import org.jetbrains.annotations.NotNull;
import today.opai.api.dataset.Vec3Data;
import today.opai.api.enums.EnumModuleCategory;
import today.opai.api.events.*;
import today.opai.api.interfaces.dataset.Vector3d;
import today.opai.api.interfaces.game.network.server.SPacket12Velocity;
import today.opai.api.interfaces.modules.values.BooleanValue;
import today.opai.api.interfaces.modules.values.ModeValue;

public class LongJumpPlus extends Module {
    private final ModeValue mode = createModes("Modes", "Watchdog", "Watchdog");
    private final BooleanValue timer = createBoolean("Timer", false);

    private boolean waitingForDamage;
    private int ticksSinceVelocity;
    private int ticks;

    public LongJumpPlus() {
        super("LongJump+", "A set of additional LongJump implementations", EnumModuleCategory.MOVEMENT);
    }

    @Override
    public void onPacketReceive(@NotNull EventPacketReceive event) {
        if (event.getPacket() instanceof SPacket12Velocity) {
            SPacket12Velocity packet = (SPacket12Velocity) event.getPacket();
            if (event.isCancelled() || !packet.isCurrentEntity()) return;

            double var5 = packet.getX() / (double)8000.0F;
            double var7 = packet.getZ() / (double)8000.0F;
            double var9 = Math.hypot(var5, var7);
            MoveUtil.strafe(MathUtils.limit(var9, 0.44, 0.48));
            Vector3d motion = player.getMotion();
            player.setMotion(new Vec3Data(
                    motion.getX(),
                    packet.getY() / (double)8000.0F,
                    motion.getZ()
            ));
            ticksSinceVelocity = 0;
            event.setCancelled(true);
            this.waitingForDamage = false;
        }
    }

    @Override
    public void onJump(EventJump event) {
        if (this.ticks < 44) {
            event.setCancelled(true);
        }
    }

    @Override
    public void onMove(EventMove event) {
        if (this.ticks < 44) {
            event.setZ(0.0F);
            event.setX(0.0F);
        }
    }

    // TODO 等待Opai更新onPreMotion
}
