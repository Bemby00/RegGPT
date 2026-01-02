package com.mirteney.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirteney.model.Account;
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

/**
 * Репозиторий для хранения аккаунтов в JSON-файле.
 *
 * <p>Обеспечивает чтение и запись массива аккаунтов в файл.</p>
 */
public class AccountRepository {

    private static final Logger log = LoggerFactory.getLogger(AccountRepository.class);

    private final Path filePath;
    private final ObjectMapper mapper;

    /**
     * Создаёт репозиторий с файловым путём по умолчанию.
     *
     * @param filePath путь к JSON-файлу
     */
    public AccountRepository(@NotNull Path filePath) {
        this(filePath, new ObjectMapper());
    }

    /**
     * Создаёт репозиторий с указанным ObjectMapper.
     *
     * @param filePath путь к JSON-файлу
     * @param mapper   ObjectMapper для сериализации
     */
    public AccountRepository(@NotNull Path filePath, @NotNull ObjectMapper mapper) {
        this.filePath = filePath;
        this.mapper = mapper;
    }

    /**
     * Возвращает все сохранённые аккаунты.
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
        return Collections.unmodifiableList(accounts);
    }

    /**
     * Сохраняет новый аккаунт в JSON-файл.
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

            List<Account> accounts = new ArrayList<>(loadAccounts(channel));
            accounts.add(account);

            // Записываем обновлённый список в временный файл и затем атомарно заменяем.
            Path parentDir = filePath.toAbsolutePath().getParent();
            if (parentDir == null) {
                parentDir = Path.of(".");
            }
            Path tempFile = Files.createTempFile(parentDir, "accounts", ".json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(tempFile.toFile(), accounts);
            Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Аккаунт сохранён: login={}", account.login());
        }
    }

    /**
     * Считывает аккаунты из канала, чтобы не конфликтовать с файловой блокировкой.
     *
     * @param channel канал файла с установленной блокировкой
     * @return неизменяемый список аккаунтов
     * @throws IOException если чтение завершилось ошибкой
     */
    private @NotNull List<Account> loadAccounts(@NotNull SeekableByteChannel channel) throws IOException {
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
        return Collections.unmodifiableList(accounts);
    }
}
