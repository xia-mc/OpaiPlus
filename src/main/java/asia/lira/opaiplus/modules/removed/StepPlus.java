package asia.lira.opaiplus.modules.removed;

import asia.lira.opaiplus.internal.Module;
import today.opai.api.enums.EnumModuleCategory;
import today.opai.api.interfaces.modules.values.ModeValue;
import today.opai.api.interfaces.modules.values.NumberValue;

public class StepPlus extends Module {
    private static final double[] MOTION = {0.42, 0.75, 1};

    private final ModeValue mode = createModes("Mode", "Watchdog", "Watchdog");
    private final NumberValue timer = createNumber("Timer", 0.25, 0.25, 1, 0.01);
    private final NumberValue delay = createNumber("Delay", 1000, 0, 2000, 100);

    private long lastStep = -1;
    private boolean stepped = false;

    public StepPlus() {
        super("Step+", "A set of additional Step implementations", EnumModuleCategory.MOVEMENT);
        setDepends(timer, mode, "Watchdog");
    }

    @Override
    public String getSuffix() {
        return mode.getValue();
    }

    // TODO 等待cubk添加setStepHeight
}
