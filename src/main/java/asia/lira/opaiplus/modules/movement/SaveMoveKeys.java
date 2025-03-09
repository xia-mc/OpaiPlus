package asia.lira.opaiplus.modules.movement;

import asia.lira.opaiplus.internal.Module;
import asia.lira.opaiplus.internal.unsafe.Unsafe;
import asia.lira.opaiplus.utils.MathUtils;
import asia.lira.opaiplus.utils.RandomUtils;
import org.lwjgl.input.Keyboard;
import today.opai.api.enums.EnumKeybind;
import today.opai.api.enums.EnumModuleCategory;
import today.opai.api.interfaces.game.Options;
import today.opai.api.interfaces.modules.values.LabelValue;
import today.opai.api.interfaces.modules.values.NumberValue;

public class SaveMoveKeys extends Module {
    @SuppressWarnings("unused")
    private final LabelValue tip = createLabel("Only WASD keys is supported.");
    private final NumberValue minDelay = createNumber("Min Delay", 0, 0, 500, 10);
    private final NumberValue maxDelay = createNumber("Max Delay", 0, 0, 500, 10);

    private final Options options = API.getOptions();
    private boolean lastInScreen = false;
    private boolean toPress = false;
    private long startTime = -1;
    private int delay = 0;

    public SaveMoveKeys() {
        super("Save Move Keys", "Re-press move keys after close screen", EnumModuleCategory.MOVEMENT);
    }

    @Override
    public void onEnabled() {
        lastInScreen = Unsafe.getCurrentScreen() != null;
        toPress = false;
        startTime = -1;
        delay = 0;
    }

    @Override
    public void onLoop() {
        MathUtils.correctValue(minDelay, maxDelay);

        boolean curInScreen = Unsafe.getCurrentScreen() != null;
        if (lastInScreen && !curInScreen && !toPress) {
            toPress = true;
            int max = maxDelay.getValue().intValue();
            if (max != 0) {
                delay = RandomUtils.randInt(minDelay.getValue().intValue(), max);
                startTime = System.currentTimeMillis();

                lastInScreen = false;
                return;
            }
        }
        if (toPress) {
            if (System.currentTimeMillis() - startTime >= delay) {
                options.setPressed(EnumKeybind.FORWARD, Keyboard.isKeyDown(Keyboard.KEY_W));
                options.setPressed(EnumKeybind.LEFT, Keyboard.isKeyDown(Keyboard.KEY_A));
                options.setPressed(EnumKeybind.BACK, Keyboard.isKeyDown(Keyboard.KEY_S));
                options.setPressed(EnumKeybind.RIGHT, Keyboard.isKeyDown(Keyboard.KEY_D));
                options.setPressed(EnumKeybind.JUMP, Keyboard.isKeyDown(Keyboard.KEY_SPACE));
                options.setPressed(EnumKeybind.SNEAK, Keyboard.isKeyDown(Keyboard.KEY_LSHIFT));
                toPress = false;
            }
        }

        lastInScreen = curInScreen;
    }
}
