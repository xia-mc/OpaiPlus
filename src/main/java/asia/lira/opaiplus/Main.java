package asia.lira.opaiplus;

import asia.lira.opaiplus.internal.unsafe.AntiCrack;
import asia.lira.opaiplus.internal.Installer;
import com.allatori.annotations.DoNotRename;
import com.allatori.annotations.Rename;


@Rename
public class Main {
    static {
        Installer.start();
        AntiCrack.THROW(null);
    }

    @DoNotRename
    public static void main(String[] args) {
    }

}
