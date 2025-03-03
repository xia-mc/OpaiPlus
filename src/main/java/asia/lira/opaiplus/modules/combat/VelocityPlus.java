package asia.lira.opaiplus.modules.combat;

import asia.lira.opaiplus.OpaiPlus;
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
    private final ModeValue mode = createModes("Mode", "Prediction", "Prediction");
    private final NumberValue reduceCount = createNumber("Reduce Count", 4, 1, 4, 1);
    private final BooleanValue notWhileUsingItem = createBoolean("Not While Using Item", true);
    private final BooleanValue debug = createBoolean("Debug", false);

    private final ModuleKillAura killAura = (ModuleKillAura) API.getModuleManager().getModule("KillAura");
    private int unReduceTimes = 0;

    public VelocityPlus() {
        super("Velocity+", "A set of additional Anti-KB implementations", EnumModuleCategory.COMBAT);
    }

    @Override
    public String getSuffix() {
        return mode.getValue();
    }

    @Override
    public void onDisabled() {
        unReduceTimes = 0;
    }

    @Override
    public void onPlayerUpdate() {
        if (mode.getValue().equals("Prediction")) {
            boolean shouldReduce = unReduceTimes > 0 && player.getHurtTime() > 0
                    && !(notWhileUsingItem.getValue() && player.isUsingItem() && !player.isBlocking())
                    && killAura.getTarget() != null && player.isSprinting();

            if (shouldReduce) {
                int tick = reduceCount.getValue().intValue() - unReduceTimes + 1;

                doReduce();
                if (debug.getValue()) {
                    Vector3d motion = player.getMotion();
                    OpaiPlus.info(String.format("%d Reduced %.3f %.3f", tick, motion.getX(), motion.getZ()));
                }
                unReduceTimes--;
            } else {
                unReduceTimes = 0;
            }
        }
    }

    @Override
    public void onPacketReceive(EventPacketReceive event) {
        if (mode.getValue().equals("Prediction")) {
            if (!(event.getPacket() instanceof SPacket12Velocity)) {
                return;
            }
            SPacket12Velocity packet = (SPacket12Velocity) event.getPacket();
            if (!packet.isCurrentEntity()) {
                return;
            }

            unReduceTimes = reduceCount.getValue().intValue();
        }
    }

    private void doReduce() {
        player.attack(killAura.getTarget());

        Vector3d motion = player.getMotion();
        player.setMotion(new Vec3Data(
                motion.getX() * 0.6,
                motion.getY(),
                motion.getZ() * 0.6
        ));
    }
}
