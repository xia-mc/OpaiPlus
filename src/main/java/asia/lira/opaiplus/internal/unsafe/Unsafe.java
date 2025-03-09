package asia.lira.opaiplus.internal.unsafe;

import asia.lira.opaiplus.utils.ReflectionUtils;
import org.jetbrains.annotations.Nullable;

public class Unsafe {
    private static final Object mcInstance = ReflectionUtils.getDeclared(
            ReflectionUtils.getClass("MatrixShield.iR"), "b");

    /**
     * MatrixShield.iR.b.A 等价于 mc.currentScreen
     */
    public static @Nullable Object getCurrentScreen() {
        return ReflectionUtils.get(mcInstance, "A");
    }
}
