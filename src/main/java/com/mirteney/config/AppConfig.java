package com.mirteney.config;

import org.jetbrains.annotations.NotNull;

/**
 * Конфигурация приложения с загрузкой параметров из config.properties.
 *
 * <p>Хранит все настраиваемые параметры приложения.</p>
 */
public class AppConfig {

    /**
     * Фиксированные URL и селекторы для работы с vten.ru.
     */
    private static final String DEFAULT_TRAINING_URL = "https://m.vten.ru/training/battle";
    private static final String DEFAULT_SAVE_URL = "https://m.vten.ru/user/save";
    private static final String DEFAULT_HERO_CLASS_SELECTOR = "text=ВЫБЕРИ КЛАСС ГЕРОЯ";
    private static final String DEFAULT_LOGIN_INPUT_SELECTOR = "input[name='loginContainer:name']";
    private static final String DEFAULT_PASSWORD_INPUT_SELECTOR = "input[name='passwordContainer:password']";
    private static final String DEFAULT_SAVE_BUTTON_SELECTOR = "button.btn-rich3._main";

    private final ConfigLoader configLoader;

    // Telegram настройки
    private final String botToken;
    private final String encryptionKey;

    // Playwright настройки
    private final String trainingUrl;
    private final String saveUrl;
    private final String heroClassSelector;
    private final String loginInputSelector;
    private final String passwordInputSelector;
    private final String saveButtonSelector;
    private final boolean headlessBrowser;
    private final int pageLoadTimeout;
    private final int maxRetries;

    /**
     * Создаёт конфигурацию с параметрами из config.properties.
     */
    public AppConfig() {
        this.configLoader = new ConfigLoader();

        // Загружаем настройки Telegram
        this.botToken = configLoader.getRequiredString("telegram.bot.token", "TELEGRAM_BOT_TOKEN");
        this.encryptionKey = configLoader.getString("encryption.key", "ENCRYPTION_KEY", "");

        // Фиксированные настройки Playwright, не зависящие от конфигурации
        this.trainingUrl = DEFAULT_TRAINING_URL;
        this.saveUrl = DEFAULT_SAVE_URL;
        this.heroClassSelector = DEFAULT_HERO_CLASS_SELECTOR;
        this.loginInputSelector = DEFAULT_LOGIN_INPUT_SELECTOR;
        this.passwordInputSelector = DEFAULT_PASSWORD_INPUT_SELECTOR;
        this.saveButtonSelector = DEFAULT_SAVE_BUTTON_SELECTOR;
        this.headlessBrowser = configLoader.getBoolean("vten.headless", "VTEN_HEADLESS", false);
        this.pageLoadTimeout = configLoader.getInt("vten.page.timeout", "VTEN_PAGE_TIMEOUT", 30000);
        this.maxRetries = configLoader.getInt("vten.max.retries", "VTEN_MAX_RETRIES", 3);
    }

    public String getBotToken() {
        return botToken;
    }

    public String getEncryptionKey() {
        return encryptionKey;
    }

    public String getTrainingUrl() {
        return trainingUrl;
    }

    public String getSaveUrl() {
        return saveUrl;
    }

    public String getHeroClassSelector() {
        return heroClassSelector;
    }

    public String getLoginInputSelector() {
        return loginInputSelector;
    }

    public String getPasswordInputSelector() {
        return passwordInputSelector;
    }

    public String getSaveButtonSelector() {
        return saveButtonSelector;
    }

    public boolean isHeadlessBrowser() {
        return headlessBrowser;
    }

    public int getPageLoadTimeout() {
        return pageLoadTimeout;
    }

    public int getMaxRetries() {
        return maxRetries;
    }
}
