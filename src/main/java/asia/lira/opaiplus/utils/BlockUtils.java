package asia.lira.opaiplus.utils;

import asia.lira.opaiplus.OpaiPlus;
import asia.lira.opaiplus.internal.unsafe.AntiCrack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectImmutableList;
import lombok.Data;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import today.opai.api.dataset.BlockPosition;
import today.opai.api.dataset.BoundingBox;
import today.opai.api.dataset.Vec3Data;
import today.opai.api.enums.EnumDirection;
import today.opai.api.interfaces.dataset.Vector3d;

import java.util.*;
import java.util.stream.Collectors;

public class BlockUtils {
    private static final Set<EnumDirection> FACINGS = new HashSet<>(new ObjectImmutableList<>(EnumDirection.values()));

    @Data
    public static final class PlaceSide {
        private final BlockPosition blockPos;
        private final EnumDirection direction;
        private final Vec3Data hitPos;
    }

    public static @NotNull BlockPosition resolve(@NotNull BlockPosition blockPos, @NotNull EnumDirection direction) {
        switch (direction) {
            case EAST:
                return new BlockPosition(blockPos.x + 1, blockPos.y, blockPos.z);
            case WEST:
                return new BlockPosition(blockPos.x - 1, blockPos.y, blockPos.z);
            case SOUTH:
                return new BlockPosition(blockPos.x, blockPos.y, blockPos.z + 1);
            case NORTH:
                return new BlockPosition(blockPos.x, blockPos.y, blockPos.z - 1);
            case UP:
                return new BlockPosition(blockPos.x, blockPos.y + 1, blockPos.z);
            case DOWN:
                return new BlockPosition(blockPos.x, blockPos.y - 1, blockPos.z);
            default:
                return AntiCrack.UNREACHABLE();
        }
    }

    public static @NotNull BlockPosition up(BlockPosition blockPos) {
        return resolve(blockPos, EnumDirection.UP);
    }

    @Contract(pure = true)
    public static @NotNull Optional<PlaceSide> getPlaceSide(@NotNull BlockPosition blockPos) {
        return getPlaceSide(blockPos, FACINGS);
    }

    @Contract(pure = true)
    public static @NotNull Optional<PlaceSide> getPlaceSide(@NotNull BlockPosition blockPos, Set<EnumDirection> limitFacing) {
        final List<BlockPosition> possible = FACINGS.stream()
                .map(direction -> resolve(blockPos, direction))
                .collect(Collectors.toList());

        for (BlockPosition pos : possible) {
            if (false) {  // TODO !BlockUtils.replaceable(pos)
                EnumDirection facing;
                Vec3Data hitPos;
                if (pos.getY() < blockPos.getY()) {
                    facing = EnumDirection.UP;
                    hitPos = new Vec3Data(pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5);
                } else if (pos.getX() > blockPos.getX()) {
                    facing = EnumDirection.WEST;
                    hitPos = new Vec3Data(pos.getX(), pos.getY() + 0.5, pos.getZ() + 0.5);
                } else if (pos.getX() < blockPos.getX()) {
                    facing = EnumDirection.EAST;
                    hitPos = new Vec3Data(pos.getX() + 1, pos.getY() + 0.5, pos.getZ() + 0.5);
                } else if (pos.getZ() < blockPos.getZ()) {
                    facing = EnumDirection.SOUTH;
                    hitPos = new Vec3Data(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 1);
                } else if (pos.getZ() > blockPos.getZ()) {
                    facing = EnumDirection.NORTH;
                    hitPos = new Vec3Data(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ());
                } else {
                    facing = EnumDirection.DOWN;
                    hitPos = new Vec3Data(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                }

                if (!limitFacing.contains(facing)) continue;

                return Optional.of(new PlaceSide(pos, facing, hitPos));
            }
        }
        return Optional.empty();
    }

    public static @NotNull List<BlockPosition> getAllInBox(@NotNull BlockPosition from, @NotNull BlockPosition to) {
        final List<BlockPosition> blocks = new ObjectArrayList<>();

        BlockPosition min = new BlockPosition(Math.min(from.getX(), to.getX()),
                Math.min(from.getY(), to.getY()), Math.min(from.getZ(), to.getZ()));
        int a = from.getZ();
        int b = to.getZ();
        int a1 = from.getY();
        int b1 = to.getY();
        int a2 = from.getX();
        int b2 = to.getX();
        BlockPosition max = new BlockPosition(Math.max(a2, b2), Math.max(a1, b1), Math.max(a, b));

        for (int x = min.getX(); x <= max.getX(); x++)
            for (int y = min.getY(); y <= max.getY(); y++)
                for (int z = min.getZ(); z <= max.getZ(); z++)
                    blocks.add(new BlockPosition(x, y, z));

        return blocks;
    }

    public static @NotNull List<BlockPosition> getAllInSphere(@NotNull BlockPosition from, double distance) {
        final int blockDistance = (int) Math.round(distance);
        final List<BlockPosition> blocks = new ObjectArrayList<>();

        for (BlockPosition blockPos : getAllInBox(
                new BlockPosition(from.x - blockDistance, from.y - blockDistance, from.z - blockDistance),
                new BlockPosition(from.x + blockDistance, from.y + blockDistance, from.z + blockDistance)
        )) {
            BoundingBox box = getCollisionBoundingBox(blockPos);
            if (box == null) continue;

            Vec3Data vec3From = Vec3Utils.create(from);
            if (Vec3Utils.distanceTo(RotationUtils.getNearestPoint(box, vec3From), vec3From) <= distance) {
                blocks.add(blockPos);
            }
        }

        return blocks;
    }

    private static BoundingBox getCollisionBoundingBox(BlockPosition blockPos) {
        return OpaiPlus.getAPI().getWorld().getBoundingBox(blockPos);
    }

    @Contract("_, _, _ -> new")
    public static @NotNull BlockPosition createPos(double x, double y, double z) {
        return new BlockPosition((int) x, (int) y, (int) z);
    }

    @Contract("_ -> new")
    public static @NotNull BlockPosition createPos(@NotNull Vector3d vector3d) {
        return createPos(vector3d.getX(), vector3d.getY(),  vector3d.getZ());
    }
}
