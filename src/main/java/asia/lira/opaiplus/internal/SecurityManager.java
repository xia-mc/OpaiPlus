package asia.lira.opaiplus.internal;

import asia.lira.opaiplus.OpaiPlus;
import lombok.Getter;
import lombok.SneakyThrows;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class SecurityManager {
    @Getter
    private static boolean securitySupported = false;
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final int KEY_SIZE = 256;
    private static final int IV_SIZE = 16;

    private static ThreadLocal<MessageDigest> digestSHA256 = null;
    private static ThreadLocal<Mac> macSHA256 = null;
    private static ThreadLocal<SecretKeyFactory> secretKeyFactory = null;
    private static ThreadLocal<Cipher> cipherAES = null;

    public static void init() {
        digestSHA256 = ThreadLocal.withInitial(() -> {
            try {
                return MessageDigest.getInstance("SHA-256");
            } catch (Exception e) {
                throw new RuntimeException("Error initializing SHA256", e);
            }
        });
        macSHA256 = ThreadLocal.withInitial(() -> {
            try {
                return Mac.getInstance("HmacSHA256");
            } catch (Exception e) {
                throw new RuntimeException("Error initializing HMAC-SHA256", e);
            }
        });
        secretKeyFactory = ThreadLocal.withInitial(() -> {
            try {
                return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            } catch (Exception e) {
                throw new RuntimeException("Error initializing PBKDF2WithHmacSHA256", e);
            }
        });
        cipherAES = ThreadLocal.withInitial(() -> {
            try {
                return Cipher.getInstance(ALGORITHM);
            } catch (Exception e) {
                throw new RuntimeException("Error initializing AES/CBC/PKCS5Padding", e);
            }
        });

        securitySupported = true;
        try {
            String ignored1 = sha256("test");
            String ignored2 = hmacSHA256("test", "test");
            String encrypted = encrypt("test", "test");
            if (!decrypt(encrypted, "test").equals("test")) {
                throw new RuntimeException();
            }
        } catch (Throwable e) {
            securitySupported = false;
            digestSHA256 = null;
            macSHA256 = null;
            OpaiPlus.error("Security algorithms is unsupported on your device. Some modules are not available.");
        }
    }

    public static @NotNull String sha256(@NotNull String input) {
        assert isSecuritySupported();
        MessageDigest digest = digestSHA256.get();
        byte[] hash = digest.digest(input.getBytes());
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            hexString.append(String.format("%02x", b));
        }
        return hexString.toString();
    }

    public static String hmacSHA256(@NotNull String data, @NotNull String key) {
        assert isSecuritySupported();
        try {
            Mac mac = macSHA256.get();
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(), "HmacSHA256");
            mac.init(secretKey);
            byte[] hmacBytes = mac.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("Error calculating HMAC-SHA256", e);
        }
    }

    @SneakyThrows
    public static String encrypt(@NotNull String plaintext, String password) {
        assert isSecuritySupported();
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt); // 生成随机盐

        SecretKey key = deriveKey(password, salt);
        Cipher cipher = cipherAES.get();
        byte[] iv = new byte[IV_SIZE];
        new SecureRandom().nextBytes(iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));

        byte[] encrypted = cipher.doFinal(plaintext.getBytes());
        byte[] combined = new byte[salt.length + iv.length + encrypted.length];

        System.arraycopy(salt, 0, combined, 0, salt.length);
        System.arraycopy(iv, 0, combined, salt.length, iv.length);
        System.arraycopy(encrypted, 0, combined, salt.length + iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    @Contract("_, _ -> new")
    public static @NotNull String decrypt(String ciphertext, String password) throws Exception {
        assert isSecuritySupported();
        byte[] combined = Base64.getDecoder().decode(ciphertext);
        byte[] salt = new byte[16];
        byte[] iv = new byte[IV_SIZE];
        byte[] encrypted = new byte[combined.length - salt.length - iv.length];

        System.arraycopy(combined, 0, salt, 0, salt.length);
        System.arraycopy(combined, salt.length, iv, 0, iv.length);
        System.arraycopy(combined, salt.length + iv.length, encrypted, 0, encrypted.length);

        SecretKey key = deriveKey(password, salt);
        Cipher cipher = cipherAES.get();
        cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));

        return new String(cipher.doFinal(encrypted));
    }

    private static @NotNull SecretKey deriveKey(@NotNull String password, byte[] salt) throws Exception {
        assert isSecuritySupported();
        SecretKeyFactory factory = secretKeyFactory.get();
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 65536, KEY_SIZE);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }
}
