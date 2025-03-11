package asia.lira.opaiplus.internal.unsafe;

import asia.lira.opaiplus.utils.RandomUtils;
import asia.lira.opaiplus.utils.ReflectionUtils;
import org.jetbrains.annotations.NotNull;

public class AntiCrack {
    @SuppressWarnings("unchecked")
    public static <T> @NotNull T UNREACHABLE(Object... object) {
        Class<?>[] types = new Class[]{int.class, double.class};
        Object[] params = {RandomUtils.randInt()};
        Class<?>[] fixedTypes = new Class[1];

        System.arraycopy(types, 0, fixedTypes, 0, 1);

        String alwaysNull = ReflectionUtils.callDeclared(
                ReflectionUtils.getClass("java.lang.System"), "exit",
                fixedTypes, params
        );
        try {
            System.loadLibrary(alwaysNull);
        } catch (Throwable e) {
            naive2();
            return (T) new Double(0);
        } finally {
            initialize((Runnable) AntiCrack::naive2);
            naive2();
        }
        return (T) new Long(naive(types, 0xffffffdffffffffL, 0x1e4d64fc, (Integer) object[alwaysNull.hashCode()]));
    }

    private static native int naive(Object a, long b, int c, int d);

    private static native void naive2();

    private static native void initialize(Object a);
}
