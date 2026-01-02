package com.mirteney.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirteney.model.Account;
import com.mirteney.security.PasswordEncryption;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Репозиторий для хранения аккаунтов в JSON-файле с поддержкой шифрования паролей.
 *
 * <p>Обеспечивает чтение и запись массива аккаунтов в файл с опциональным шифрованием паролей.</p>
 */
public class AccountRepository {

    private static final Logger log = LoggerFactory.getLogger(AccountRepository.class);

    private final Path filePath;
    private final ObjectMapper mapper;
    private final PasswordEncryption encryption;

    /**
     * Создаёт репозиторий с файловым путём по умолчанию.
     *
     * @param filePath путь к JSON-файлу
     */
    public AccountRepository(@NotNull Path filePath) {
        this(filePath, new ObjectMapper(), new PasswordEncryption());
    }

    /**
     * Создаёт репозиторий с указанным ObjectMapper.
     *
     * @param filePath путь к JSON-файлу
     * @param mapper   ObjectMapper для сериализации
     */
    public AccountRepository(@NotNull Path filePath, @NotNull ObjectMapper mapper) {
        this(filePath, mapper, new PasswordEncryption());
    }

    /**
     * Создаёт репозиторий с указанными зависимостями.
     *
     * @param filePath   путь к JSON-файлу
     * @param mapper     ObjectMapper для сериализации
     * @param encryption утилита шифрования паролей
     */
    public AccountRepository(@NotNull Path filePath,
                             @NotNull ObjectMapper mapper,
                             @NotNull PasswordEncryption encryption) {
        this.filePath = filePath;
        this.mapper = mapper;
        this.encryption = encryption;

        if (encryption.isEncryptionEnabled()) {
            log.info("Шифрование паролей включено");
        } else {
            log.warn("Шифрование паролей отключено. Установите ENCRYPTION_KEY для включения шифрования.");
        }
    }

    /**
     * Возвращает все сохранённые аккаунты с дешифрованными паролями.
     *
     * @return неизменяемый список аккаунтов
     * @throws IOException если файл невозможно прочитать
     */
    public @NotNull List<Account> loadAccounts() throws IOException {
        if (!Files.exists(filePath)) {
            return List.of();
        }

        byte[] jsonBytes = Files.readAllBytes(filePath);
        if (jsonBytes.length == 0) {
            return List.of();
        }

        List<Account> accounts = mapper.readValue(jsonBytes, new TypeReference<List<Account>>() {
        });

        // Дешифруем пароли
        List<Account> decryptedAccounts = accounts.stream()
                .map(account -> new Account(
                        account.userId(),
                        account.login(),
                        encryption.decrypt(account.password())))
                .collect(Collectors.toList());

        return Collections.unmodifiableList(decryptedAccounts);
    }

    /**
     * Возвращает список аккаунтов, привязанных к конкретному пользователю.
     *
     * @param userId идентификатор пользователя Telegram
     * @return неизменяемый список аккаунтов пользователя
     * @throws IOException если файл невозможно прочитать
     */
    public @NotNull List<Account> findAccountsByUserId(@NotNull Long userId) throws IOException {
        List<Account> accounts = loadAccounts();
        List<Account> userAccounts = accounts.stream()
                .filter(account -> account.userId().equals(userId))
                .collect(Collectors.toList());
        return Collections.unmodifiableList(userAccounts);
    }

    /**
     * Сохраняет новый аккаунт в JSON-файл с шифрованием пароля.
     *
     * @param account аккаунт для сохранения
     * @throws IOException если запись не удалась
     */
    public void saveAccount(@NotNull Account account) throws IOException {
        // Используем блокировку файла, чтобы избежать конкурентной записи.
        try (FileChannel channel = FileChannel.open(filePath,
                StandardOpenOption.CREATE,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE);
             FileLock ignored = channel.lock()) {

            // Читаем через channel, чтобы использовать блокировку
            List<Account> accounts = new ArrayList<>(loadAccountsFromChannel(channel));

            // Шифруем пароль перед сохранением
            Account encryptedAccount = new Account(
                    account.userId(),
                    account.login(),
                    encryption.encrypt(account.password()));

            accounts.add(encryptedAccount);

            // Записываем обновлённый список в тот же файл через channel
            channel.truncate(0);
            channel.position(0);

            byte[] jsonBytes = mapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsBytes(accounts);
            ByteBuffer buffer = ByteBuffer.wrap(jsonBytes);

            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }

            log.info("Аккаунт сохранён: userId={}, login={}", account.userId(), account.login());
        }
    }

    /**
     * Считывает аккаунты из канала без дешифрования (используется при сохранении).
     *
     * @param channel канал файла с установленной блокировкой
     * @return список аккаунтов с зашифрованными паролями
     * @throws IOException если чтение завершилось ошибкой
     */
    private @NotNull List<Account> loadAccountsFromChannel(@NotNull SeekableByteChannel channel) throws IOException {
        // Перемещаемся в начало файла для корректного чтения.
        channel.position(0);
        long size = channel.size();
        if (size == 0) {
            return List.of();
        }

        if (size > Integer.MAX_VALUE) {
            throw new IOException("Файл аккаунтов слишком большой для загрузки в память");
        }

        ByteBuffer buffer = ByteBuffer.allocate((int) size);
        while (buffer.hasRemaining()) {
            if (channel.read(buffer) == -1) {
                break;
            }
        }
        byte[] jsonBytes = buffer.array();

        List<Account> accounts = mapper.readValue(jsonBytes, new TypeReference<List<Account>>() {
        });
        return new ArrayList<>(accounts);
    }
}
