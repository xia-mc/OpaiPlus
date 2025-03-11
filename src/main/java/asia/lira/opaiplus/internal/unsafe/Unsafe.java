package asia.lira.opaiplus.internal.unsafe;

import asia.lira.opaiplus.utils.ReflectionUtils;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public class Unsafe {
    @Getter
    private static final Object mcInstance = ReflectionUtils.getDeclared(
            ReflectionUtils.getClass("MatrixShield.iR"), "b");

    @SuppressWarnings({"unchecked", "DataFlowIssue"})
    @Contract(value = "_ -> fail", pure = true)
    public static <T extends Throwable> void THROW(@Nullable Object throwable) throws T {
        throw (T) throwable;
    }

    public static @Nullable Object getCurrentScreen() {
        return ReflectionUtils.get(mcInstance, "A");
    }

    public static void setLeftClickCounter(int leftClickCounter) {
        ReflectionUtils.set(mcInstance, "c", leftClickCounter);
    }
}
