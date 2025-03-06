package asia.lira.opaiplus.utils;

import asia.lira.opaiplus.OpaiPlus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import today.opai.api.dataset.PositionData;
import today.opai.api.interfaces.game.entity.Entity;
import today.opai.api.interfaces.game.entity.LocalPlayer;

public class MathUtils {
    private static final LocalPlayer player = OpaiPlus.getAPI().getLocalPlayer();

    public static boolean posEquals(double first, double second) {
        if (first == second) {
            return true;
        }

        return Math.abs(first - second) < 0.001;
    }

    public static double limit(double value, double min, double max) {
        return Math.min(Math.max(value, min), max);
    }

    public static int limit(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    public static boolean inFov(float fov, @NotNull Entity entity) {
        PositionData position = entity.getPosition();
        return inFov(fov, position.getX(), position.getZ());
    }

    public static boolean inFov(float fov, final double x, final double z) {
        fov *= 0.5F;
        final double fovToPoint = getFov(x, z);
        if (fovToPoint > 0.0) {
            return fovToPoint < fov;
        } else return fovToPoint > -fov;
    }

    public static @Range(from = -180, to = 180) double getFov(final double posX, final double posZ) {
        return getFov(player.getRotation().getYaw(), posX, posZ);
    }

    public static @Range(from = -180, to = 180) double getFov(final float yaw, final double posX, final double posZ) {
        return wrapAngleTo180((yaw - angle(posX, posZ)) % 360.0f);
    }

    public static float angle(final double x, final double z) {
        return (float) (Math.atan2(x - player.getPosition().getX(), z - player.getPosition().getZ()) * 57.295780181884766 * -1.0);
    }

    public static double wrapAngleTo180(double angle) {
        angle %= 360.0; // 限制在 [-360, 360] 范围内
        if (angle >= 180.0) {
            angle -= 360.0; // 使角度落入 [-180, 180]
        } else if (angle < -180.0) {
            angle += 360.0;
        }
        return angle;
    }

}
