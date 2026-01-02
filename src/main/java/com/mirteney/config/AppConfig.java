package com.mirteney.config;

import org.jetbrains.annotations.NotNull;

/**
 * Конфигурация приложения с загрузкой параметров из config.properties.
 *
 * <p>Хранит все настраиваемые параметры приложения.</p>
 */
public class AppConfig {

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

        // Загружаем настройки Playwright
        this.trainingUrl = configLoader.getString("vten.training.url", "VTEN_TRAINING_URL", "https://m.vten.ru/training/battle");
        this.saveUrl = configLoader.getString("vten.save.url", "VTEN_SAVE_URL", "https://m.vten.ru/user/save");
        this.heroClassSelector = configLoader.getString("vten.hero.class.selector", "VTEN_HERO_CLASS_SELECTOR", "text=ВЫБЕРИ КЛАСС ГЕРОЯ");
        this.loginInputSelector = configLoader.getString("vten.login.selector", "VTEN_LOGIN_SELECTOR", "input[name='loginContainer:name']");
        this.passwordInputSelector = configLoader.getString("vten.password.selector", "VTEN_PASSWORD_SELECTOR", "input[name='passwordContainer:password']");
        this.saveButtonSelector = configLoader.getString("vten.save.button.selector", "VTEN_SAVE_BUTTON_SELECTOR", "button.btn-rich3._main");
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
