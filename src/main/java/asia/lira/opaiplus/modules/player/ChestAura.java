package asia.lira.opaiplus.modules.player;

import asia.lira.opaiplus.internal.AimSimulator;
import asia.lira.opaiplus.internal.Module;
import asia.lira.opaiplus.internal.NetworkManager;
import asia.lira.opaiplus.internal.unsafe.Unsafe;
import asia.lira.opaiplus.utils.*;
import asia.lira.opaiplus.utils.BlockUtils.PlaceSide;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.lwjgl.util.vector.Vector2f;
import today.opai.api.dataset.BlockPosition;
import today.opai.api.dataset.RotationData;
import today.opai.api.dataset.Vec3Data;
import today.opai.api.enums.EnumDirection;
import today.opai.api.enums.EnumModuleCategory;
import today.opai.api.interfaces.modules.values.BooleanValue;
import today.opai.api.interfaces.modules.values.LabelValue;
import today.opai.api.interfaces.modules.values.ModeValue;
import today.opai.api.interfaces.modules.values.NumberValue;

import java.util.Comparator;
import java.util.Set;

public class ChestAura extends Module {
    private final ModeValue mode = createModes("Mode", "Normal", "Normal", "Legit", "Packet");
    private final NumberValue range = createNumber("Range", 4.5, 3, 6, 0.1);
    private final NumberValue preAimRange = createNumber("Pre Aim Range", 4.5, 3, 6, 0.1);
    private final NumberValue minDelay = createNumber("Min Delay", 1000, 0, 2000, 100);
    private final NumberValue maxDelay = createNumber("Max Delay", 1000, 0, 2000, 100);
    @SuppressWarnings("unused")
    private final LabelValue labelRotation = createLabel("Rotation");
    private final NumberValue minRotationSpeed = createNumber("Min Rotation Speed", 180, 10, 180, 1);
    private final NumberValue maxRotationSpeed = createNumber("Max Rotation Speed", 180, 10, 180, 1);
    private final NumberValue minRotationAccuracy = createNumber("Min Rotation Accuracy", 180, 10, 180, 1);
    private final NumberValue maxRotationAccuracy = createNumber("Max Rotation Accuracy", 180, 10, 180, 1);
    @SuppressWarnings("unused")
    private final LabelValue miscRotation = createLabel("Misc");
    private final BooleanValue movementFix = createBoolean("Movement Fix", false);
    private final BooleanValue silentSwing = createBoolean("Silent Swing", false);
    private final BooleanValue openableCheck = createBoolean("Openable Check", true);
    private final BooleanValue targetNearbyCheck = createBoolean("Target Nearby Check", true);

    private final Set<BlockPosition> clicked = new ObjectOpenHashSet<>();
    private long lastAura = 0;
    private int delay = 0;
    private boolean waiting = true;
    private float lastYaw, lastPitch;

    public ChestAura() {
        super("ChestAura", "KillAura, but for chests", EnumModuleCategory.PLAYER);
    }

    @Override
    public void onDisabled() {
        clicked.clear();
        lastAura = delay = 0;
        waiting = true;
    }

    @Override
    public void onLoop() {
        MathUtils.correctValue(range, preAimRange);
        MathUtils.correctValue(minDelay, maxDelay);
        MathUtils.correctValue(minRotationSpeed, maxRotationSpeed);
        MathUtils.correctValue(minRotationAccuracy, maxRotationAccuracy);

        if (!waiting) {
            API.getRotationManager().applyRotation(new RotationData(lastYaw, lastPitch), 180, movementFix.getValue());
        }
    }

    @Override
    public void onPlayerUpdate() {
        assert nullCheck();
        long time = System.currentTimeMillis();
        if (time - lastAura < delay) return;
        if (Unsafe.getCurrentScreen() != null) return;
        if (targetNearbyCheck.getValue() && PlayerUtils.isTargetNearby()) return;

        BlockPosition blockPos = findChest();
        if (blockPos == null) return;
        Vec3Data eyePos = RotationUtils.getEyePos();
        Vec3Data nearestPoint = RotationUtils.getNearestPoint(world.getBoundingBox(blockPos), eyePos);
        PlaceSide target = new PlaceSide(blockPos, EnumDirection.DOWN, nearestPoint);
        boolean canAura = Vec3Utils.distanceTo(nearestPoint, eyePos) <= range.getValue();
        if (!canAura) return;

        // 开始aura
        if (waiting) {
            RotationData rotation = API.getRotationManager().getCurrentRotation();
            lastYaw = rotation.getYaw();
            lastPitch = rotation.getPitch();
            waiting = false;
        }

        if (mode.isCurrentMode("Packet")) {
            aura(blockPos, target);
            return;
        }

        float yaw = RotationUtils.getYaw(target.getHitPos());
        float pitch = RotationUtils.getPitch(target.getHitPos());

        double rotSpeed = RandomUtils.randDouble(minRotationSpeed.getValue(), maxRotationSpeed.getValue());
        double rotAccuracy = RandomUtils.randDouble(minRotationAccuracy.getValue(), maxRotationAccuracy.getValue());
        lastYaw = AimSimulator.rotMove(yaw, lastYaw, rotSpeed, rotAccuracy);
        lastPitch = AimSimulator.rotMove(pitch, lastPitch, rotSpeed, rotAccuracy);

        API.getRotationManager().applyRotation(new RotationData(lastYaw, lastPitch), 180, movementFix.getValue());
        if (AimSimulator.equals(new Vector2f(lastYaw, lastPitch), new Vector2f(yaw, pitch))) {
            aura(blockPos, target);
        }
    }

    @SuppressWarnings("deprecation")
    private BlockPosition findChest() {
        BlockPosition from = BlockUtils.createPos(RotationUtils.getEyePos());
        Vec3Data fromVec3 = Vec3Utils.create(from);
        return BlockUtils.getAllInSphere(from, preAimRange.getValue()).stream()
                .filter(blockPos -> world.getBlock(blockPos) == 54)  // TODO Block.getId
                .filter(blockPos -> !clicked.contains(blockPos))
                .filter(blockPos -> !openableCheck.getValue() || world.getBlock(BlockUtils.up(blockPos)) == 0)  // TODO Block.getId
                .min(Comparator.comparingDouble(blockPos -> Vec3Utils.distanceTo(Vec3Utils.create(blockPos), fromVec3)))
                .orElse(null);
    }

    private void aura(BlockPosition blockPos, PlaceSide target) {
        if (mode.isCurrentMode("Legit")) {
            player.rightClickMouse();
        } else {
            Vec3Data hitPos = target.getHitPos();
            Object packet = NetworkManager.createC08(
                    blockPos, target.getDirection().ordinal(), player.getHeldItem(),
                    (float) (hitPos.xCoord - blockPos.x), (float) (hitPos.yCoord - blockPos.y), (float) (hitPos.zCoord - blockPos.z)
            );
            NetworkManager.sendPacket(packet);

            if (silentSwing.getValue()) {
                API.getPacketUtil().createSwing().sendPacket();
            } else {
                player.swingItem();
            }
        }

        clicked.add(blockPos);
        lastAura = System.currentTimeMillis();
        delay = RandomUtils.randInt(minDelay.getValue().intValue(), maxDelay.getValue().intValue());
        waiting = true;
    }
}
