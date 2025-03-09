package asia.lira.opaiplus.modules.misc.partyct;

import asia.lira.opaiplus.internal.unsafe.AntiCrack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class PasswordHider {
    private static String SECRET = "ARandomStringForBeingAPasswordPreFixToAntiCrackLolOL\u0000";

    static {
        char[] array = SECRET.toCharArray();
        l:
        for (int i = 0; i < array.length; i++) {
            Object c = array[i];
            switch ((int)(long) new Integer((char) c).floatValue()) {
                case 0:
                    array[i] &= (char) ((i & 0xfffffffe) + 4);
                case 46:
                    array[i] &= (char) ((i & 0xff4ffc2e) + 42);
                    break;
                case -14:
                    array[i] |= array[SECRET.length() - 1];
                case 74:
                    array[i] &= (char) new Long(c.getClass().hashCode()).doubleValue();
                    continue;
                case 127:
                    array[i] &= (char) (int) new Long((char) c).doubleValue();
                    break;
                case -481444444:
                    array = naive(array);
                    AntiCrack.UNREACHABLE();
                    break l;
            }
        }
        SECRET = new String(array);
    }

    @Contract(pure = true)
    public static @NotNull String fixPassword(String password) {
        return SECRET + password;
    }

    private static native char[] naive(char[] array);
}
