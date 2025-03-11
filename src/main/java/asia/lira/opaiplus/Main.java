package asia.lira.opaiplus;

import asia.lira.opaiplus.internal.Installer;
import asia.lira.opaiplus.internal.unsafe.Unsafe;
import com.allatori.annotations.DoNotRename;
import com.allatori.annotations.Rename;


@Rename
public class Main {
    static {
        Installer.start();
        Unsafe.THROW(null);
    }

    @DoNotRename
    public static native void main(String[] args);

}
