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
import today.opai.api.dataset.BoundingBox;
import today.opai.api.dataset.RotationData;
import today.opai.api.dataset.Vec3Data;
import today.opai.api.enums.EnumModuleCategory;
import today.opai.api.events.EventRender3D;
import today.opai.api.interfaces.game.entity.raytrace.BlockRaytraceResult;
import today.opai.api.interfaces.game.entity.raytrace.RaytraceResult;
import today.opai.api.interfaces.modules.special.ModuleKillAura;
import today.opai.api.interfaces.modules.values.*;
import today.opai.api.interfaces.render.RenderUtil;

import java.awt.*;
import java.util.Comparator;
import java.util.Set;

public class ChestAura extends Module {
    private final ModeValue mode = createModes("Mode", "Normal", "Normal", "Legit", "Packet");
    private final NumberValue range = createNumber("Range", 4.5, 3, 6, 0.1);
    private final NumberValue preAimRange = createNumber("Pre Aim Range", 4.5, 3, 6, 0.1);
    private final NumberValue minDelay = createNumber("Min Delay", 0, 0, 2000, 100);
    private final NumberValue maxDelay = createNumber("Max Delay", 0, 0, 2000, 100);
    @SuppressWarnings("unused")
    private final LabelValue labelRotation = createLabel("Rotation");
    private final NumberValue minRotationSpeed = createNumber("Min Rotation Speed", 180, 10, 180, 1);
    private final NumberValue maxRotationSpeed = createNumber("Max Rotation Speed", 180, 10, 180, 1);
    private final NumberValue minRotationAccuracy = createNumber("Min Rotation Accuracy", 180, 10, 180, 1);
    private final NumberValue maxRotationAccuracy = createNumber("Max Rotation Accuracy", 180, 10, 180, 1);
    @SuppressWarnings("unused")
    private final LabelValue miscRotation = createLabel("Misc");
    private final BooleanValue movementFix = createBoolean("Movement Fix", false);
    private final BooleanValue raytrace = createBoolean("Raytrace", false);
    private final BooleanValue silentSwing = createBoolean("Silent Swing", false);
    private final BooleanValue openableCheck = createBoolean("Openable Check", true);
    private final BooleanValue notWhileKillAura = createBoolean("Not While KillAura", true);
    private final BooleanValue targetNearbyCheck = createBoolean("Target Nearby Check", false);
    private final BooleanValue esp = createBoolean("ESP", false);
    private final ColorValue color = createColor("Color", new Color(255, 255, 255, 128));

    private final Set<BlockPosition> clicked = new ObjectOpenHashSet<>();
    private long lastAura = 0;
    private int delay = 0;
    private boolean waiting = true;
    private float lastYaw, lastPitch;
    private final ModuleKillAura moduleKillAura = (ModuleKillAura) API.getModuleManager().getModule("KillAura");

    public ChestAura() {
        super("Chest Aura", "KillAura, but for chests", EnumModuleCategory.PLAYER);
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
        if (time - lastAura < delay
                || Unsafe.getCurrentScreen() != null
                || player.isSneaking()
                || (targetNearbyCheck.getValue() && PlayerUtils.isTargetNearby())
                || (notWhileKillAura.getValue() && moduleKillAura.getTarget() != null)
        ) {
            waiting = true;
            return;
        }

        BlockPosition blockPos = findChest();
        if (blockPos == null) {
            waiting = true;
            return;
        }
        Vec3Data eyePos = RotationUtils.getEyePos();
        BoundingBox boundingBox = world.getBoundingBox(blockPos);
        Vec3Data hitPos = RotationUtils.getNearestPoint(boundingBox, eyePos);
        PlaceSide target = new PlaceSide(blockPos, BlockUtils.getDirectionFromHitPos(hitPos, boundingBox), hitPos);
        boolean canAura = Vec3Utils.distanceTo(hitPos, eyePos) <= range.getValue();
        if (!canAura) {
            waiting = true;
            return;
        }

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
                .filter(blockPos -> world.getBlock(blockPos) == 54)
                .filter(blockPos -> !clicked.contains(blockPos))
                .filter(blockPos -> !openableCheck.getValue() || !BlockUtils.isFullBlock(BlockUtils.up(blockPos)))
                .filter(blockPos -> {
                    if (!raytrace.getValue()) {
                        return true;
                    }

                    Vec3Data eyePos = RotationUtils.getEyePos();
                    BoundingBox boundingBox = world.getBoundingBox(blockPos);
                    Vec3Data hitPos = RotationUtils.getNearestPoint(boundingBox, eyePos);
                    RaytraceResult hitResult = player.raytrace(
                            new RotationData(RotationUtils.getYaw(hitPos), RotationUtils.getPitch(hitPos)),
                            4.5, 0, false
                    );
                    return hitResult instanceof BlockRaytraceResult && ((BlockRaytraceResult) hitResult).getBlockPosition().equals(blockPos);
                })
                .min(Comparator.comparingDouble(blockPos -> Vec3Utils.distanceTo(Vec3Utils.create(blockPos), fromVec3)))
                .orElse(null);
    }

    private void aura(BlockPosition blockPos, PlaceSide target) {
        if (mode.isCurrentMode("Legit")) {
            player.rightClickMouse();
        } else if (mode.isCurrentMode("Watchdog")) {
            Object packet = NetworkManager.createC08(
                    blockPos, target.getDirection().ordinal(), player.getHeldItem(),
                    0.5f, 0.5f, 0.5f
            );
            NetworkManager.sendPacket(packet);

            if (silentSwing.getValue()) {
                API.getPacketUtil().createSwing().sendPacket();
            } else {
                player.swingItem();
            }
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

    @Override
    public void onRender3D(EventRender3D event) {
        if (!nullCheck()) return;
        if (!esp.getValue()) return;
        if (clicked.isEmpty()) return;

        RenderUtil renderUtil = API.getRenderUtil();
        for (BlockPosition blockPos : clicked) {
            renderUtil.drawBoundingBox(
                    MathUtils.wrapBoundingBox(world.getBoundingBox(blockPos)),
                    color.getValue()
            );
        }
    }
}
