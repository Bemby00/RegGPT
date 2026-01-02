package com.mirteney.config;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Загрузчик конфигурации из файла config.properties.
 *
 * <p>Загружает настройки из файла, с fallback на переменные окружения.</p>
 */
public class ConfigLoader {

    private static final Logger log = LoggerFactory.getLogger(ConfigLoader.class);
    private static final String CONFIG_FILE = "config.properties";

    private final Properties properties;

    /**
     * Создаёт загрузчик и загружает конфигурацию.
     */
    public ConfigLoader() {
        this.properties = loadProperties();
    }

    /**
     * Загружает properties из файла.
     *
     * @return объект Properties
     */
    private Properties loadProperties() {
        Properties props = new Properties();
        Path configPath = Path.of(CONFIG_FILE);

        if (Files.exists(configPath)) {
            try (InputStream input = new FileInputStream(configPath.toFile())) {
                props.load(input);
                log.info("Конфигурация загружена из файла: {}", CONFIG_FILE);
            } catch (IOException e) {
                log.warn("Не удалось загрузить конфигурацию из файла: {}. Используются переменные окружения.", CONFIG_FILE, e);
            }
        } else {
            log.warn("Файл конфигурации не найден: {}. Используются переменные окружения.", CONFIG_FILE);
        }

        return props;
    }

    /**
     * Получает строковое значение из конфигурации.
     *
     * @param key          ключ свойства
     * @param envKey       имя переменной окружения (fallback)
     * @param defaultValue значение по умолчанию
     * @return значение свойства
     */
    public @NotNull String getString(@NotNull String key, String envKey, String defaultValue) {
        // Приоритет: 1) config.properties, 2) переменная окружения, 3) значение по умолчанию
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            value = System.getenv(envKey);
        }
        if (value == null || value.isBlank()) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Получает обязательное строковое значение из конфигурации.
     *
     * @param key    ключ свойства
     * @param envKey имя переменной окружения (fallback)
     * @return значение свойства
     * @throws IllegalStateException если значение не найдено
     */
    public @NotNull String getRequiredString(@NotNull String key, String envKey) {
        String value = getString(key, envKey, null);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(
                    "Обязательный параметр не найден: " + key +
                    (envKey != null ? " (или переменная окружения " + envKey + ")" : ""));
        }
        return value;
    }

    /**
     * Получает булево значение из конфигурации.
     *
     * @param key          ключ свойства
     * @param envKey       имя переменной окружения (fallback)
     * @param defaultValue значение по умолчанию
     * @return значение свойства
     */
    public boolean getBoolean(@NotNull String key, String envKey, boolean defaultValue) {
        String value = getString(key, envKey, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    /**
     * Получает целочисленное значение из конфигурации.
     *
     * @param key          ключ свойства
     * @param envKey       имя переменной окружения (fallback)
     * @param defaultValue значение по умолчанию
     * @return значение свойства
     */
    public int getInt(@NotNull String key, String envKey, int defaultValue) {
        String value = getString(key, envKey, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            log.warn("Некорректное значение для {}: {}. Используется значение по умолчанию: {}",
                    key, value, defaultValue);
            return defaultValue;
        }
    }
}
