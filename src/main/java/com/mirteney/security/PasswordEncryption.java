package com.mirteney.security;

import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Optional;

/**
 * Утилита для шифрования и дешифрования паролей с использованием AES-GCM.
 *
 * <p>Использует ключ шифрования, полученный из переменной окружения ENCRYPTION_KEY.
 * Если ключ не задан, шифрование не применяется (fallback для обратной совместимости).</p>
 */
public class PasswordEncryption {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int SALT_LENGTH = 16;
    private static final int ITERATION_COUNT = 65536;
    private static final int KEY_LENGTH = 256;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;
    private final boolean encryptionEnabled;

    /**
     * Создаёт экземпляр с ключом из конфигурации или переменной окружения.
     */
    public PasswordEncryption() {
        this.secureRandom = new SecureRandom();

        // Приоритет: config.properties -> переменная окружения
        String encryptionPassword = null;
        try {
            com.mirteney.config.AppConfig config = new com.mirteney.config.AppConfig();
            encryptionPassword = config.getEncryptionKey();
        } catch (Exception e) {
            // Если не удалось загрузить конфиг, пробуем переменную окружения
            encryptionPassword = System.getenv("ENCRYPTION_KEY");
        }

        if (encryptionPassword != null && !encryptionPassword.isBlank()) {
            byte[] salt = deriveSalt(encryptionPassword);
            this.secretKey = deriveKey(encryptionPassword, salt);
            this.encryptionEnabled = true;
        } else {
            this.secretKey = null;
            this.encryptionEnabled = false;
        }
    }

    /**
     * Создаёт экземпляр с заданным паролем для шифрования.
     *
     * @param encryptionPassword пароль для генерации ключа шифрования
     */
    public PasswordEncryption(@NotNull String encryptionPassword) {
        this.secureRandom = new SecureRandom();
        if (!encryptionPassword.isBlank()) {
            byte[] salt = deriveSalt(encryptionPassword);
            this.secretKey = deriveKey(encryptionPassword, salt);
            this.encryptionEnabled = true;
        } else {
            this.secretKey = null;
            this.encryptionEnabled = false;
        }
    }

    /**
     * Проверяет, включено ли шифрование.
     *
     * @return true, если шифрование включено
     */
    public boolean isEncryptionEnabled() {
        return encryptionEnabled;
    }

    /**
     * Шифрует пароль.
     *
     * @param password открытый пароль
     * @return зашифрованный пароль в Base64 или исходный пароль, если шифрование отключено
     */
    public @NotNull String encrypt(@NotNull String password) {
        if (!encryptionEnabled) {
            return password;
        }

        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);

            byte[] encryptedData = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));

            // Объединяем IV и зашифрованные данные
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + encryptedData.length);
            byteBuffer.put(iv);
            byteBuffer.put(encryptedData);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при шифровании пароля", e);
        }
    }

    /**
     * Дешифрует пароль.
     *
     * @param encryptedPassword зашифрованный пароль в Base64
     * @return дешифрованный пароль или исходную строку, если шифрование отключено
     */
    public @NotNull String decrypt(@NotNull String encryptedPassword) {
        if (!encryptionEnabled) {
            return encryptedPassword;
        }

        try {
            byte[] decodedData = Base64.getDecoder().decode(encryptedPassword);
            ByteBuffer byteBuffer = ByteBuffer.wrap(decodedData);

            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            byte[] encryptedBytes = new byte[byteBuffer.remaining()];
            byteBuffer.get(encryptedBytes);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);

            byte[] decryptedData = cipher.doFinal(encryptedBytes);
            return new String(decryptedData, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при дешифровании пароля", e);
        }
    }

    /**
     * Создаёт детерминированную соль из пароля для шифрования.
     *
     * @param password пароль
     * @return соль
     */
    private byte[] deriveSalt(@NotNull String password) {
        byte[] salt = new byte[SALT_LENGTH];
        byte[] passwordBytes = password.getBytes(StandardCharsets.UTF_8);

        for (int i = 0; i < SALT_LENGTH; i++) {
            salt[i] = passwordBytes[i % passwordBytes.length];
        }
        return salt;
    }

    /**
     * Создаёт ключ шифрования из пароля и соли.
     *
     * @param password пароль
     * @param salt     соль
     * @return секретный ключ
     */
    private SecretKey deriveKey(@NotNull String password, byte[] salt) {
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");
        } catch (Exception e) {
            throw new RuntimeException("Ошибка при создании ключа шифрования", e);
        }
    }
}
