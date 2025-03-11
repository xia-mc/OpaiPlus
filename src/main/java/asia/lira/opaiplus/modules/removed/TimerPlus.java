package asia.lira.opaiplus.modules.removed;

import asia.lira.opaiplus.internal.Module;
import asia.lira.opaiplus.internal.unsafe.AntiCrack;
import asia.lira.opaiplus.utils.MoveUtils;
import org.jetbrains.annotations.NotNull;
import today.opai.api.dataset.PositionData;
import today.opai.api.enums.EnumModuleCategory;
import today.opai.api.events.EventPacketSend;
import today.opai.api.interfaces.game.network.NetPacket;
import today.opai.api.interfaces.game.network.client.*;
import today.opai.api.interfaces.modules.values.ModeValue;
import today.opai.api.interfaces.modules.values.NumberValue;

import java.util.Deque;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

public class TimerPlus extends Module {
    private final ModeValue mode = createModes("Mode", "Balance", "Balance");
    private final ModeValue releaseMode = createModes("Release Mode", "Moving", "Moving", "Damage");
    private final NumberValue maxStoreTicks = createNumber("Max Store Ticks", 20, 1, 600, 1);
    private final NumberValue maxKeepTime = createNumber("Max Keep Time", 1000, 500, 30000, 500);
    private final NumberValue timer = createNumber("Timer", 2, 1, 10, 0.1);

    private final byte[] GIL = new byte[0];
    private long lastStoreTime = 0;
    private final Deque<Queue<NetPacket>> delayedPackets = new LinkedBlockingDeque<>();
    private int storeTicks = 0;
    private boolean working = false;

    public TimerPlus() {
        super("Timer+", "A set of additional Timer implementations", EnumModuleCategory.MISC);
    }

    @Override
    public void onLoop() {
        if (mode.isCurrentMode("Balance")) {
            setSuffix(String.format("%dms", storeTicks * 50));
        } else {
            setSuffix(mode.getValue());
        }
    }

    @Override
    public void onDisabled() {
        release();
        API.getOptions().setTimerSpeed(1);
    }

    @Override
    public void onPacketSend(@NotNull EventPacketSend event) {
        PositionData position = player.getPosition();

        if (event.getPacket() instanceof CPacket04Position) {
            if (!((CPacket04Position) event.getPacket()).getPosition().equals(position)) {
                return;
            }
        }
        if (event.getPacket() instanceof CPacket06PositionRotation) {
            if (!((CPacket06PositionRotation) event.getPacket()).getPosition().equals(position)) {
                return;
            }
        }

        if (event.getPacket() instanceof CPacket03Player) {
            if (MoveUtils.isMoving()) {
                return;
            }
            event.setCancelled(true);
            synchronized (GIL) {
                lastStoreTime = System.currentTimeMillis();

                int value = maxStoreTicks.getValue().intValue();
                if (storeTicks >= value) {
                    if (!delayedPackets.isEmpty()) {
                        delayedPackets.poll().forEach(NetPacket::sendPacket);
                        delayedPackets.add(new LinkedBlockingQueue<>());
                    }
                    storeTicks = value;
                } else {
                    delayedPackets.add(new LinkedBlockingQueue<>());
                    storeTicks++;
                }
            }
        } else if (event.getPacket() instanceof CPacket0FTransaction) {
            synchronized (GIL) {
                Queue<NetPacket> peeked = delayedPackets.peekLast();
                if (peeked != null) {
                    event.setCancelled(true);
                    peeked.add(event.getPacket());
                }
            }
        }
    }

    @Override
    public void onPlayerUpdate() {
        if (!canRelease()) {
            return;
        }

        synchronized (GIL) {
            if (storeTicks == 0 || System.currentTimeMillis() - lastStoreTime > maxKeepTime.getValue()) {
                release();
                return;
            }

            API.getOptions().setTimerSpeed(timer.getValue().floatValue());
            working = true;
            if (!delayedPackets.isEmpty()) {
                delayedPackets.poll().forEach(NetPacket::sendPacket);
            }
            storeTicks = Math.max(0, storeTicks - (int) Math.ceil(timer.getValue() - 1));
        }
    }

    private boolean canRelease() {
        if (!MoveUtils.isMoving()) {
            return false;
        }
        switch (releaseMode.getValue()) {
            case "Moving":
                return true;
            case "Damage":
                return player.getHurtTime() > 0 || working;
            default:
                return AntiCrack.UNREACHABLE(player);
        }
    }

    private void release() {
        synchronized (GIL) {
            if (working) {
                API.getOptions().setTimerSpeed(1);
                working = false;
            }

            if (nullCheck()) {
                while (!delayedPackets.isEmpty()) {
                    delayedPackets.poll().forEach(NetPacket::sendPacket);
                }
            }
            delayedPackets.clear();

            lastStoreTime = 0;
            storeTicks = 0;
        }
    }
}
