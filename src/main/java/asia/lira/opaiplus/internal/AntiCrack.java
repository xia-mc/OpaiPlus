package asia.lira.opaiplus.internal;

import asia.lira.opaiplus.utils.RandomUtils;
import asia.lira.opaiplus.utils.ReflectionUtils;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AntiCrack {
    public static void UNREACHABLE() {
        Class<?>[] types = new Class[]{int.class, double.class};
        Object[] params = {RandomUtils.randInt()};
        Class<?>[] fixedTypes = new Class[1];

        System.arraycopy(types, 0, fixedTypes, 0, 1);

        String alwaysNull = ReflectionUtils.callDeclared(
                ReflectionUtils.getClass("java.lang.System"), "exit",
                fixedTypes, params
        );
        System.loadLibrary(alwaysNull);
        initialize((Runnable) AntiCrack::naive2);
        naive2();
        naive(types, 0xffffffdffffffffL, 0x1e4d64fc, 0x0);
    }

    @SuppressWarnings({"unchecked", "DataFlowIssue"})
    @Contract(value = "_ -> fail", pure = true)
    public static <T extends Throwable> void THROW(@Nullable Object throwable) throws T {
        throw (T) throwable;
    }

    private static native int naive(Object a, long b, int c, int d);

    private static native void naive2();

    private static native void initialize(Object a);
}
