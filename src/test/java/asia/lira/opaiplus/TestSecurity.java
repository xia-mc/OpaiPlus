package asia.lira.opaiplus;

import asia.lira.opaiplus.internal.SecurityManager;
import asia.lira.opaiplus.modules.misc.partyct.OpCode;
import asia.lira.opaiplus.modules.misc.partyct.PasswordHider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestSecurity {

    @Test
    public void testEncrypt() throws Exception {
        SecurityManager.init();

        String string = String.format("%d,%s", OpCode.HEARTBEAT, "xia__mc");
        String encrypted = SecurityManager.encrypt(string, "Global");
        System.out.printf("Encrypted: %s\n", encrypted);
        String result = SecurityManager.decrypt(encrypted, "Global");
        System.out.printf("Decrypted: %s\n", result);

        Assertions.assertEquals(string, result);
    }

    @Test
    public void testFixedEncrypt() throws Exception {
        SecurityManager.init();

        String string = String.format("%d,%s", OpCode.HEARTBEAT, "xia__mc");
        String encrypted = SecurityManager.encrypt(string, PasswordHider.fixPassword("Global"));
        System.out.printf("Encrypted: %s\n", encrypted);
        String result = SecurityManager.decrypt(encrypted, PasswordHider.fixPassword("Global"));
        System.out.printf("Decrypted: %s\n", result);

        Assertions.assertEquals(string, result);
    }
}
