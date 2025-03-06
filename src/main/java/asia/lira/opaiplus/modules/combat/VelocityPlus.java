package asia.lira.opaiplus.modules.combat;

import asia.lira.opaiplus.OpaiPlus;
import asia.lira.opaiplus.internal.AntiCrack;
import asia.lira.opaiplus.internal.Module;
import today.opai.api.dataset.Vec3Data;
import today.opai.api.enums.EnumModuleCategory;
import today.opai.api.events.EventPacketReceive;
import today.opai.api.interfaces.dataset.Vector3d;
import today.opai.api.interfaces.game.network.server.SPacket12Velocity;
import today.opai.api.interfaces.modules.special.ModuleKillAura;
import today.opai.api.interfaces.modules.values.BooleanValue;
import today.opai.api.interfaces.modules.values.ModeValue;
import today.opai.api.interfaces.modules.values.NumberValue;

public class VelocityPlus extends Module {
    private final ModeValue mode = createModes("Mode", "Prediction", "Prediction", "GrimAC");
    private final NumberValue reduceAmount = createNumber("Reduce Amount", 4, 1, 6, 1);
    private final NumberValue packets = createNumber("Packets", 4, 1, 6, 1);
    private final NumberValue ticks = createNumber("Ticks", 1, 1, 6, 1);
    private final BooleanValue notWhileUsingItem = createBoolean("Not While Using Item", true);
    private final BooleanValue legit = createBoolean("Legit", false);
    private final BooleanValue debug = createBoolean("Debug", false);

    private final ModuleKillAura killAura = (ModuleKillAura) API.getModuleManager().getModule("KillAura");
    private int reduceTicks = -1;

    public VelocityPlus() {
        super("Velocity+", "A set of additional Anti-KB implementations", EnumModuleCategory.COMBAT);
        setDepends(reduceAmount, mode, "Prediction");
        setDepends(packets, mode, "GrimAC");
        setDepends(ticks, mode, "GrimAC");
    }

    @Override
    public String getSuffix() {
        return mode.getValue();
    }

    @Override
    public void onDisabled() {
        reduceTicks = -1;
    }

    @Override
    public void onPlayerUpdate() {
        if (!nullCheck()) return;
        if (mode.isCurrentMode("Prediction") || mode.isCurrentMode("GrimAC")) {
            boolean shouldReduce = reduceTicks != -1 && player.getHurtTime() > 0
                    && !(notWhileUsingItem.getValue() && player.isUsingItem() && !player.isBlocking())
                    && killAura.getTarget() != null && player.isSprinting();

            switch (mode.getValue()) {
                case "Prediction":
                    shouldReduce &= reduceTicks < reduceAmount.getValue().intValue();
                    break;
                case "GrimAC":
                    shouldReduce &= reduceTicks < ticks.getValue().intValue();
                    break;
                default:
                    AntiCrack.UNREACHABLE();
                    break;
            }

            if (shouldReduce) {
                switch (mode.getValue()) {
                    case "Prediction":
                        doReduce();
                        break;
                    case "GrimAC":
                        for (int i = 0; i < packets.getValue().intValue(); i++) {
                            doReduce();
                        }
                        break;
                    default:
                        AntiCrack.UNREACHABLE();
                        break;
                }

                if (debug.getValue()) {
                    Vector3d motion = player.getMotion();
                    OpaiPlus.log(String.format("%d Reduced %.3f %.3f", reduceTicks + 1, motion.getX(), motion.getZ()));
                }
                reduceTicks++;
            } else {
                reduceTicks = -1;
            }
        }
    }

    @Override
    public void onPacketReceive(EventPacketReceive event) {
        if (mode.isCurrentMode("Prediction") || mode.isCurrentMode("GrimAC")) {
            if (!(event.getPacket() instanceof SPacket12Velocity)) {
                return;
            }
            SPacket12Velocity packet = (SPacket12Velocity) event.getPacket();
            if (!packet.isCurrentEntity()) {
                return;
            }

            reduceTicks = 0;
        }
    }

    private void doReduce() {
        if (legit.getValue()) {
            player.clickMouse();
        } else {
            player.attack(killAura.getTarget());

            Vector3d motion = player.getMotion();
            player.setMotion(new Vec3Data(
                    motion.getX() * 0.6,
                    motion.getY(),
                    motion.getZ() * 0.6
            ));
        }
    }
}
