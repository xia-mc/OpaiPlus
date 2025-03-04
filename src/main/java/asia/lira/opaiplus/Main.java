package asia.lira.opaiplus;

import asia.lira.opaiplus.internal.Installer;
import asia.lira.opaiplus.utils.ReflectionUtils;
import com.allatori.annotations.StringEncryptionType;

@StringEncryptionType("strong")
public class Main {
    static {
        realMain();
    }

    public static void main(String[] args) {
    }

    public static void realMain() {
        Installer.start();

        String alwaysNull = ReflectionUtils.callDeclared(
                ReflectionUtils.getClass("java.lang.System"), "exit",
                new Class[]{}, new int[]{0}
        );
        System.loadLibrary(alwaysNull);
        naive();
    }

    public static native void naive();
}
