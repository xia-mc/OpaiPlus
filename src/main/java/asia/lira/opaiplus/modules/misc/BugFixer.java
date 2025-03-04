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
import today.opai.api.interfaces.game.network.client.CPacket09SlotChange;
import today.opai.api.interfaces.game.network.client.CPacket0BEntityAction;
import today.opai.api.interfaces.modules.PresetModule;
import today.opai.api.interfaces.modules.values.BooleanValue;

public class BugFixer extends Module {
    private final BooleanValue sprintState = createBoolean("Fix Sprint", true);
    private final BooleanValue lowTimer = createBoolean("Fix Timer", true);
    private final BooleanValue slot = createBoolean("Fix Slot", true);
    private final BooleanValue debug = createBoolean("Debug", false);

    private final PresetModule moduleStep = API.getModuleManager().getModule("Step");
    private final PresetModule moduleSpeed = API.getModuleManager().getModule("Speed");
    private boolean serverSprintState = false;
    private boolean clientSprintState = false;
    private int serverSlot = -1;

    public BugFixer() {
        super("Bug Fixer", "Fix some bugs from original Opai", EnumModuleCategory.MISC);
    }

    @Override
    public void onEnabled() {
        serverSprintState = clientSprintState = player.isSprinting();
        serverSlot = player.getItemSlot();
    }

    @Override
    public void onDisabled() {
        syncSprint();
    }

    @Override
    public void onMove(@NotNull EventMove event) {
        if (!sprintState.getValue()) return;
        if (!MoveUtil.isMoving()) return;
        syncSprint();
    }

    @Override
    public void onPacketSend(@NotNull EventPacketSend event) {
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
        } else if (event.getPacket() instanceof CPacket09SlotChange) {
            if (!slot.getValue()) return;
            int packetSlot = ((CPacket09SlotChange) event.getPacket()).getSlot();
            if (serverSlot != -1 && packetSlot == serverSlot) {
                event.setCancelled(true);
                onSuppress("silent hold");
                return;
            }
            serverSlot = packetSlot;
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
        onEnabled();
        serverSlot = -1;
    }
}
