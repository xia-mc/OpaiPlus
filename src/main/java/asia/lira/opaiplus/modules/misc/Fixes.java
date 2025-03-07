package asia.lira.opaiplus.modules.misc;

import asia.lira.opaiplus.OpaiPlus;
import asia.lira.opaiplus.internal.Module;
import asia.lira.opaiplus.internal.NetworkManager;
import asia.lira.opaiplus.utils.MathUtils;
import asia.lira.opaiplus.utils.MoveUtil;
import org.jetbrains.annotations.NotNull;
import today.opai.api.enums.EnumEntityAction;
import today.opai.api.enums.EnumModuleCategory;
import today.opai.api.events.EventMove;
import today.opai.api.events.EventPacketSend;
import today.opai.api.events.EventRender2D;
import today.opai.api.interfaces.game.network.client.CPacket0BEntityAction;
import today.opai.api.interfaces.modules.PresetModule;
import today.opai.api.interfaces.modules.values.BooleanValue;
import today.opai.api.interfaces.modules.values.NumberValue;

public class Fixes extends Module {
    private final BooleanValue sprintState = createBoolean("Sprint", true);
    private final BooleanValue lowTimer = createBoolean("Timer", true);
    private final BooleanValue airStrafe = createBoolean("Air Strafe (Low FPS)", false);
    private final NumberValue keepTicks = createNumber("Keep Ticks", 10, 5, 20, 1);
    private final BooleanValue debug = createBoolean("Debug", false);

    private final PresetModule moduleStep = API.getModuleManager().getModule("Step");
    private final PresetModule moduleSpeed = API.getModuleManager().getModule("Speed");
    private final BooleanValue moduleSpeedAirStrafe = moduleSpeed.getValues().stream()
            .filter(value -> value.getName().equals("Air Strafe"))
            .findAny()
            .map(value -> (BooleanValue) value)
            .orElseThrow(() -> new RuntimeException("Can't find Air Strafe option in Module Speed."));

    private boolean serverSprintState = false;
    private boolean clientSprintState = false;
    private boolean initializing = false;

    private float lastYaw;
    private int strafeTicks = 0;
    private Boolean lastStrafeEnabled = null;

    public Fixes() {
        super("Fixes", "Fix something from original Opai", EnumModuleCategory.MISC);
        setDepends(keepTicks, airStrafe);
    }

    @Override
    public void onEnabled() {
        initializing = true;

        ensureInitialized();
    }

    private boolean ensureInitialized() {
        if (!initializing) return true;
        if (!nullCheck()) return false;
        serverSprintState = clientSprintState = player.isSprinting();
        lastYaw = MoveUtil.directionYaw();
        strafeTicks = 0;
        lastStrafeEnabled = null;
        initializing = false;
        return true;
    }

    @Override
    public void onDisabled() {
        if (!ensureInitialized()) {
            return;
        }
        if (lastStrafeEnabled != null) {
            moduleSpeedAirStrafe.setValue(lastStrafeEnabled);
        }
        syncSprint();
    }

    @Override
    public void onTick() {
        if (!ensureInitialized()) return;
        if (!airStrafe.getValue()) return;
        if (!nullCheck()) return;
        float curYaw = MoveUtil.directionYaw();

        if (MoveUtil.isMoving() && (curYaw == lastYaw || Math.abs(curYaw - lastYaw) < 15)) {
            if (lastStrafeEnabled == null) {
                lastStrafeEnabled = moduleSpeedAirStrafe.getValue();
            }
            if (strafeTicks > 0) {
                strafeTicks--;
            } else {
                moduleSpeedAirStrafe.setValue(false);
            }
        } else if (lastStrafeEnabled != null) {
            moduleSpeedAirStrafe.setValue(lastStrafeEnabled);
            strafeTicks = keepTicks.getValue().intValue();
            lastStrafeEnabled = null;
        }

        lastYaw = curYaw;
    }

    @Override
    public void onMove(@NotNull EventMove event) {
        if (!ensureInitialized()) {
            return;
        }
        if (!sprintState.getValue()) return;
        if (!MoveUtil.isMoving()) return;
        syncSprint();
    }

    @Override
    public void onPacketSend(@NotNull EventPacketSend event) {
        if (!ensureInitialized()) {
            return;
        }
        if (event.getPacket() instanceof CPacket0BEntityAction) {
            if (!sprintState.getValue()) return;
            CPacket0BEntityAction packet = (CPacket0BEntityAction) event.getPacket();
            if (packet.getAction() == EnumEntityAction.START_SPRINTING) {
                clientSprintState = true;
                if (serverSprintState) {
                    event.setCancelled(true);
                    return;
                }

                if (!MoveUtil.isMoving()) {
                    event.setCancelled(true);
                    onSuppress("sprint");
                    return;
                }

                serverSprintState = true;
            } else if (packet.getAction() == EnumEntityAction.STOP_SPRINTING) {
                clientSprintState = false;
                if (!serverSprintState) {
                    event.setCancelled(true);
                    return;
                }

                serverSprintState = false;
            }
        }
    }

    @Override
    public void onRender2D(EventRender2D event) {
        if (!lowTimer.getValue()) return;
        if (!(moduleStep.isEnabled() && moduleSpeed.isEnabled())) return;
        if (!MathUtils.posEquals(API.getOptions().getTimerSpeed(), 0.2)) return;

        API.getOptions().setTimerSpeed(1);
        onSuppress("timer");
    }

    private void onSuppress(String type) {
        if (debug.getValue()) {
            OpaiPlus.log(String.format("Suppressed a %s bug.", type));
        }
    }

    private void syncSprint() {
        if (serverSprintState && !clientSprintState) {
            NetworkManager.createStopSprint().sendPacketNoEvent();
            serverSprintState = false;
        } else if (!serverSprintState && clientSprintState) {
            NetworkManager.createStartSprint().sendPacketNoEvent();
            serverSprintState = true;
        }
    }

    @Override
    public void onLoadWorld() {
        initializing = true;
    }
}
