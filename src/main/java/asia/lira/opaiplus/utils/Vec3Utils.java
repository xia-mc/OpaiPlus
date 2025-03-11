package asia.lira.opaiplus.utils;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import today.opai.api.dataset.BlockPosition;
import today.opai.api.dataset.Vec3Data;
import today.opai.api.interfaces.dataset.Vector3d;

public class Vec3Utils {

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull Vec3Data create(@NotNull BlockPosition blockPos) {
        return new Vec3Data(blockPos.x, blockPos.y, blockPos.z);
    }

    @Contract("_ -> new")
    public static @NotNull Vec3Data create(@NotNull Vector3d vector3d) {
        return new Vec3Data(vector3d.getX(), vector3d.getY(), vector3d.getZ());
    }

    public static double distanceTo(@NotNull Vec3Data left, @NotNull Vec3Data right) {
        double deltaX = left.xCoord - right.xCoord;
        double deltaY = left.yCoord - right.yCoord;
        double deltaZ = left.zCoord - right.zCoord;
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }

    public static double distanceTo(@NotNull Vector3d left, @NotNull Vector3d right) {
        double deltaX = left.getX() - right.getX();
        double deltaY = left.getY() - right.getY();
        double deltaZ = left.getZ() - right.getZ();
        return Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);
    }
}
