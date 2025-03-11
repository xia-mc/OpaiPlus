package asia.lira.opaiplus.utils;

import asia.lira.opaiplus.OpaiPlus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import today.opai.api.dataset.BoundingBox;
import today.opai.api.dataset.PositionData;
import today.opai.api.dataset.Vec3Data;
import today.opai.api.interfaces.game.entity.Entity;
import today.opai.api.interfaces.game.entity.LocalPlayer;
import today.opai.api.interfaces.modules.values.NumberValue;

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

    public static float normalize(float yaw) {
        return normalize(yaw, -180, 180);
    }

    /**
     * normalize the yaw from min to max.
     * @return min <= yaw < max
     */
    public static float normalize(float yaw, float min, float max) {
        yaw %= 360.0F;
        if (yaw >= max) {
            yaw -= 360.0F;
        }
        if (yaw < min) {
            yaw += 360.0F;
        }

        return yaw;
    }

    public static void correctValue(@NotNull NumberValue min, @NotNull NumberValue max) {
        double minValue, maxValue;
        if ((minValue = min.getValue()) <= (maxValue = max.getValue()))
            return;
        min.setValue(maxValue);
        max.setValue(minValue);
    }

    public static double wrapValue(double value, double lastValue, float partialTicks) {
        return lastValue + (value - lastValue) * partialTicks;
    }

    public static @NotNull BoundingBox wrapBoundingBox(@NotNull BoundingBox boundingBox) {
        float partialTicks = OpaiPlus.getAPI().getRenderUtil().getPartialTicks();

        PositionData position = player.getPosition();
        PositionData lastTickPosition = player.getLastTickPosition();
        double x = wrapValue(position.getX(), lastTickPosition.getX(), partialTicks);
        double y = wrapValue(position.getY(), lastTickPosition.getY(), partialTicks);
        double z = wrapValue(position.getZ(), lastTickPosition.getZ(), partialTicks);

        Vec3Data min = boundingBox.getMin();
        Vec3Data max = boundingBox.getMax();

        min = new Vec3Data(min.xCoord - x, min.yCoord - y, min.zCoord - z);
        max = new Vec3Data(max.xCoord - x, max.yCoord - y, max.zCoord - z);

        return new BoundingBox(min, max);
    }
}
