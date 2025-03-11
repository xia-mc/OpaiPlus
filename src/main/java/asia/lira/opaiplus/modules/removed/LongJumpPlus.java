package asia.lira.opaiplus.modules.removed;

import asia.lira.opaiplus.internal.Module;
import asia.lira.opaiplus.utils.MathUtils;
import asia.lira.opaiplus.utils.MoveUtils;
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
    private final BooleanValue autoDisable = createBoolean("Auto Disable", false);

    private boolean waitingForDamage;
    private int ticksSinceVelocity;
    private int ticks;

    public LongJumpPlus() {
        super("Long Jump+", "A set of additional LongJump implementations", EnumModuleCategory.MOVEMENT);
    }

    @Override
    public String getSuffix() {
        return mode.getValue();
    }

    @Override
    public void onPacketReceive(@NotNull EventPacketReceive event) {
        if (event.getPacket() instanceof SPacket12Velocity) {
            SPacket12Velocity packet = (SPacket12Velocity) event.getPacket();
            if (event.isCancelled() || !packet.isCurrentEntity()) return;

            double var5 = packet.getX() / (double)8000.0F;
            double var7 = packet.getZ() / (double)8000.0F;
            double var9 = Math.hypot(var5, var7);
            MoveUtils.strafe(MathUtils.limit(var9, 0.44, 0.48));
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

    @Override
    public void onMotionUpdate(EventMotionUpdate event) {
        ++this.ticks;
        if (this.ticks < 44 && this.ticks % 2 == 0) {
//            event.setX(event.getX() - (double)MoveUtil.nv() * 0.09);
//            event.setZ(event.getZ() - (double)MoveUtil.nw() * 0.09);
            event.setX(event.getX() - 0.02);
            event.setZ(event.getZ() - 0.08);
        }

        if (this.ticks == 1) {
            event.setY(event.getY() + 0.03495);
            event.setGround(false);
        } else if (this.ticks < 45) {
            event.setY(event.getY() + (this.ticks % 2 != 0 ? 0.1449999 : Math.random() / 5000));
            event.setGround(false);
            if (this.ticks == 44) {
                MoveUtils.strafe(MoveUtils.getAllowedHorizontalDistance() - 0.005);
                player.jump();
            }
        } else if (this.ticks == 45) {
            event.setY(event.getY() + 1.0E-13);
            API.getPacketUtil().createPlayer(true).sendPacket();
            if (this.timer.getValue()) {
                API.getOptions().setTimerSpeed(0.5F);
            }
        } else if (autoDisable.getValue() && ticks > 50) {
            if (player.isOnGround()) {
                setEnabled(false);
                return;
            }
        }

        Vector3d motion = player.getMotion();
        double motionX = motion.getX();
        double motionY = motion.getY();
        double motionZ = motion.getZ();

        if (ticksSinceVelocity > 3 && ticksSinceVelocity < 38 || ticksSinceVelocity > 37 && motionY <= 0) {
            motionY += 0.0283;
        }

        switch (ticksSinceVelocity) {
            case 1:
                motionX *= 2.1;
                motionZ *= 2.1;
                break;
            case 6:
            case 7:
                motionY = 0.03;
                break;
            case 12:
                motionY = 0.0F;
                break;
            case 13:
                motionY += 0.01;
                break;
        }

        player.setMotion(new Vec3Data(motionX, motionY, motionZ));
    }

    @Override
    public void onEnabled() {
        this.waitingForDamage = true;
        this.ticks = 0;
    }

    @Override
    public void onDisabled() {
        MoveUtils.stop();
        API.getOptions().setTimerSpeed(1);
    }
}
