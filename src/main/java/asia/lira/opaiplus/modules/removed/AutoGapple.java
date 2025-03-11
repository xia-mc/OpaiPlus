package asia.lira.opaiplus.modules.removed;

import asia.lira.opaiplus.internal.Module;
import asia.lira.opaiplus.utils.Vec3Utils;
import org.jetbrains.annotations.NotNull;
import today.opai.api.dataset.Vec3Data;
import today.opai.api.enums.EnumModuleCategory;
import today.opai.api.events.*;
import today.opai.api.interfaces.game.item.ItemStack;
import today.opai.api.interfaces.game.network.NetPacket;
import today.opai.api.interfaces.game.network.client.*;
import today.opai.api.interfaces.game.network.server.SPacket12Velocity;
import today.opai.api.interfaces.modules.special.ModuleKillAura;
import today.opai.api.interfaces.modules.values.BooleanValue;
import today.opai.api.interfaces.modules.values.NumberValue;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AutoGapple extends Module {
    private final NumberValue minHealth = createNumber("Min Health", 12, 1, 20, 1);
    private final NumberValue sendDelay = createNumber("Send Delay", 3, 2, 10, 1);
    private final NumberValue stuckDelay = createNumber("Stuck Delay", 20, 2, 20, 1);
    private final NumberValue delay = createNumber("Delay", 0, 0, 500, 50);
    private final BooleanValue onlyWhileKillAura = createBoolean("Only While KillAura", false);
    private final BooleanValue stopMovement = createBoolean("Stop Movement", false);

    private long lastEat = 0;
    private boolean eating = false;
    private int movingPackets = 0;
    private int slot = 0;
    private final Queue<NetPacket> packets = new ConcurrentLinkedQueue<>();
    private boolean needSkip = false;
    private Vec3Data motion = null;
    private final ModuleKillAura moduleKillAura = (ModuleKillAura) API.getModuleManager().getModule("KillAura");

    public AutoGapple() {
        super("Auto Gapple", "Auto heal feature for Quick Macro", EnumModuleCategory.COMBAT);
    }

    @Override
    public void onEnabled() {
        packets.clear();
        slot = -1;
        needSkip = false;
        movingPackets = 0;
        eating = false;
        motion = null;
    }

    @Override
    public void onDisabled() {
        eating = false;
        release();

        if (motion != null) {
            player.setMotion(motion);
        }
    }

    @Override
    public void onLoadWorld() {
        eating = false;
        release();
    }

    private void release() {
        while (!packets.isEmpty()) {
            final NetPacket packet = packets.poll();

            if (packet instanceof CPacket01Chat
                    || packet instanceof CPacket08Placement
                    || packet instanceof CPacket07Digging)
                continue;

            packet.sendPacketNoEvent();
        }

        movingPackets = 0;
    }

    @Override
    public void onMoveInput(EventMoveInput event) {
        if (eating && stopMovement.getValue()) {
            event.setForward(0);
            event.setStrafe(0);
        }
    }

    @Override
    public void onMove(EventMove event) {
        if (shouldFreeze()) {
            if (motion == null) {
                motion = Vec3Utils.create(player.getMotion());
            } else {
                player.setMotion(motion);
            }
            event.setX(0);
            event.setY(0);
            event.setZ(0);
            return;
        }

        if (needSkip) {
            needSkip = false;
        }
        if (motion != null) {
            player.setMotion(motion);
            motion = null;
        }
    }

    private boolean shouldFreeze() {
        return eating && player.getTicksExisted() % stuckDelay.getValue().intValue() != 0 && !needSkip;
    }

    @Override
    public void onMotionUpdate(EventMotionUpdate event) {
        // pre
        if (!nullCheck() || player.getHealth() == 0) {
            eating = false;
            packets.clear();

            return;
        }

        if ((onlyWhileKillAura.getValue() && moduleKillAura.getTarget() == null)
                || System.currentTimeMillis() - lastEat < delay.getValue()) {
            eating = false;
            release();

            return;
        }

        slot = getFoodSlot();

        if (slot == -1 || player.getHealth() >= minHealth.getValue()) {
            if (eating) {
                eating = false;
                release();
            }
        } else {
            eating = true;
            if (movingPackets >= 32) {
                int curSlot = player.getItemSlot() - 1;  // 💀 bro的getItemSlot是从1开始数的
                boolean toSwitch = slot != curSlot;
                if (toSwitch) {
                    API.getPacketUtil().createSwitchItem(slot).sendPacketNoEvent();
                }
                API.getPacketUtil().createUseItem(player.getInventory().getMainInventory().get(slot)).sendPacketNoEvent();
                player.setItemInUseCount(player.getItemInUseCount() - 32);
                release();
                if (toSwitch) {
                    API.getPacketUtil().createSwitchItem(curSlot).sendPacketNoEvent();
                }
                lastEat = System.currentTimeMillis();
            } else if (player.getTicksExisted() % sendDelay.getValue().intValue() == 0) {
                while (!packets.isEmpty()) {
                    final NetPacket packet = packets.poll();

                    if (packet instanceof CPacket01Chat) {
                        break;
                    }

                    if (packet instanceof CPacket03Player) {
                        movingPackets--;
                    }

                    packet.sendPacketNoEvent();
                }
            }
        }

        // post
        if (eating) {
            movingPackets++;
        }
    }

    @Override
    public void onPacketSend(EventPacketSend event) {
        if (!nullCheck()) return;

        NetPacket packet = event.getPacket();

        if (packet instanceof CPacket01Chat) return;

        if (!(packet instanceof CPacket09SlotChange
                || packet instanceof CPacket0EClickWindow
                || packet instanceof CPacket0DCloseWindow)) {
            if (eating) {
                event.setCancelled(true);

                packets.add(packet);
            }
        }

        if (packet instanceof CPacket03Player) {
            if (shouldFreeze()) {
                event.setCancelled(true);
            }
        }
    }

    @Override
    public void onPacketReceive(@NotNull EventPacketReceive event) {
        NetPacket packet = event.getPacket();

        if (packet instanceof SPacket12Velocity) {
            if (((SPacket12Velocity) packet).getEntityId() == player.getEntityId()) {
                needSkip = true;
            }
        }
    }

    public int getFoodSlot() {
        int slot = -1;
        for (int i = 0; i < 9; ++i) {
            final ItemStack getStackInSlot = player.getInventory().getMainInventory().get(i);
            if (getStackInSlot != null && getStackInSlot.getName().equals("item.appleGold")) {
                slot = i;
                break;
            }
        }
        return slot;
    }
}
