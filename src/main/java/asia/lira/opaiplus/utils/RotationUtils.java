package asia.lira.opaiplus.utils;

import asia.lira.opaiplus.OpaiPlus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import today.opai.api.dataset.BlockPosition;
import today.opai.api.dataset.BoundingBox;
import today.opai.api.dataset.PositionData;
import today.opai.api.dataset.Vec3Data;
import today.opai.api.interfaces.game.entity.LocalPlayer;
import today.opai.api.interfaces.game.entity.Player;

public class RotationUtils {
    public static float getYaw(@NotNull BlockPosition pos) {
        return getYaw(new Vec3Data(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
    }

    public static float getYaw(@NotNull Player from, @NotNull Vec3Data pos) {
        PositionData position = from.getPosition();
        float yaw = from.getRotation().getYaw();
        return yaw + (float) MathUtils.wrapAngleTo180(
                (float) Math.toDegrees(Math.atan2(pos.zCoord - position.getZ(), pos.xCoord - position.getX())) - 90f - yaw
        );
    }

    public static float getYaw(@NotNull Vec3Data pos) {
        return getYaw(OpaiPlus.getAPI().getLocalPlayer(), pos);
    }

    public static float getPitch(@NotNull BlockPosition pos) {
        return getPitch(new Vec3Data(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5));
    }

    public static float getPitch(@NotNull Player from, @NotNull Vec3Data pos) {
        PositionData position = from.getPosition();
        double diffX = pos.xCoord - position.getX();
        double diffY = pos.yCoord - (position.getY() + from.getEyeHeight());
        double diffZ = pos.zCoord - position.getZ();

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);

        float pitch = from.getRotation().getPitch();
        return pitch + (float) MathUtils.wrapAngleTo180((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)) - pitch);
    }

    public static float getPitch(@NotNull Vec3Data pos) {
        return getPitch(OpaiPlus.getAPI().getLocalPlayer(), pos);
    }

    @Contract("_, _ -> new")
    public static @NotNull Vec3Data getNearestPoint(@NotNull BoundingBox from, @NotNull Vec3Data to) {
        double pointX, pointY, pointZ;

        Vec3Data min = from.getMin();
        Vec3Data max = from.getMax();
        if (to.xCoord >= max.getX()) {
            pointX = max.getX();
        } else pointX = Math.max(to.xCoord, min.getX());
        if (to.yCoord >= max.getY()) {
            pointY = max.getY();
        } else pointY = Math.max(to.yCoord, min.getZ());
        if (to.zCoord >= max.getZ()) {
            pointZ = max.getZ();
        } else pointZ = Math.max(to.zCoord, min.getZ());

        return new Vec3Data(pointX, pointY, pointZ);
    }

    public static @NotNull Vec3Data getEyePos() {
        LocalPlayer player = OpaiPlus.getAPI().getLocalPlayer();
        PositionData position = player.getPosition();
        return new Vec3Data(position.getX(), position.getY() + player.getEyeHeight(), position.getZ());
    }
}
