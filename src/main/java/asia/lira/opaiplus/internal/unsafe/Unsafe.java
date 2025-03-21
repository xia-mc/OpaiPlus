package asia.lira.opaiplus.internal.unsafe;

import asia.lira.opaiplus.utils.ReflectionUtils;
import lombok.Getter;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

public class Unsafe {
    @Getter
    private static final Class<?> mcInstanceClass = ReflectionUtils.getClass("MatrixShield.sC");
    @Getter
    private static final Object mcInstance = ReflectionUtils.getDeclared(
            ReflectionUtils.getClass("MatrixShield.iX"), "l");

    @SuppressWarnings({"unchecked", "DataFlowIssue"})
    @Contract(value = "_ -> fail", pure = true)
    public static <T extends Throwable> void THROW(@Nullable Object throwable) throws T {
        throw (T) throwable;
    }

    public static @Nullable Object getCurrentScreen() {
        return ReflectionUtils.get(mcInstance, "Ill");
    }

    public static void setLeftClickCounter(int leftClickCounter) {
        ReflectionUtils.set(mcInstance, "lI", leftClickCounter);
    }
}
