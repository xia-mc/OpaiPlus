package asia.lira.opaiplus.utils;

import asia.lira.opaiplus.OpaiPlus;
import today.opai.api.dataset.Vec3Data;
import today.opai.api.interfaces.dataset.Vector3d;
import today.opai.api.interfaces.game.entity.LocalPlayer;

public class MoveUtil {
    private static final LocalPlayer player = OpaiPlus.getAPI().getLocalPlayer();

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isMoving() {
        return player.getMoveForward() != 0 || player.getMoveStrafing() != 0;
    }

    public static void strafe() {
        strafe(speed());
    }

    public static void strafe(final double speed) {
        final double yaw = direction();
        Vector3d motion = player.getMotion();
        player.setMotion(new Vec3Data(
                -Math.sin((float) yaw) * speed,
                motion.getY(),
                Math.cos((float) yaw) * speed
        ));
    }

    public static void strafe(final double speed, float yaw) {
        yaw = (float) Math.toRadians(yaw);
        Vector3d motion = player.getMotion();
        player.setMotion(new Vec3Data(
                -Math.sin(yaw) * speed,
                motion.getY(),
                Math.cos(yaw) * speed
        ));
    }

    public static double speed() {
        Vector3d motion = player.getMotion();
        return Math.hypot(motion.getX(), motion.getZ());
    }

    public static double direction() {
        return Math.toRadians(directionYaw());
    }

    public static float directionYaw() {
        float rotationYaw = player.getRotation().getYaw();

        if (player.getMoveForward() < 0) {
            rotationYaw += 180;
        }

        float forward = 1;

        if (player.getMoveForward() < 0) {
            forward = -0.5F;
        } else if (player.getMoveForward() > 0) {
            forward = 0.5F;
        }

        if (player.getMoveStrafing() > 0) {
            rotationYaw -= 70 * forward;
        }

        if (player.getMoveStrafing() < 0) {
            rotationYaw += 70 * forward;
        }
        return rotationYaw;
    }
}
