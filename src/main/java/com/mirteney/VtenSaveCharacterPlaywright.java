package com.mirteney;

import com.microsoft.playwright.*;
import com.mirteney.config.AppConfig;
import com.mirteney.model.Account;
import com.mirteney.repository.AccountRepository;
import com.mirteney.service.AccountService;
import com.mirteney.service.PasswordGenerator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Автоматизация процесса выбора героя и сохранения персонажа с помощью Playwright.
 *
 * <p>Программа открывает страницу выбора класса героя, переходит на страницу сохранения
 * персонажа, заполняет поле пароля и нажимает кнопку сохранения с механизмом повторных попыток.</p>
 */
public class VtenSaveCharacterPlaywright {

    private static final Logger log = LoggerFactory.getLogger(VtenSaveCharacterPlaywright.class);
    private static final long DEFAULT_USER_ID = 1L; // Для standalone запуска

    private final AppConfig config;
    private final AccountService accountService;
    private final AccountRepository accountRepository;

    /**
     * Создаёт экземпляр с зависимостями по умолчанию.
     */
    public VtenSaveCharacterPlaywright() {
        this(new AppConfig(),
                new AccountService(new PasswordGenerator()),
                new AccountRepository(Path.of("accounts.json")));
    }

    /**
     * Создаёт экземпляр с внедрением зависимостей.
     *
     * @param config             конфигурация приложения
     * @param accountService     сервис генерации аккаунтов
     * @param accountRepository  репозиторий для сохранения данных
     */
    public VtenSaveCharacterPlaywright(@NotNull AppConfig config,
                                       @NotNull AccountService accountService,
                                       @NotNull AccountRepository accountRepository) {
        this.config = config;
        this.accountService = accountService;
        this.accountRepository = accountRepository;
    }

    /**
     * Точка входа приложения с демонстрацией автоматизации.
     *
     * @param args аргументы командной строки
     */
    public static void main(@NotNull String[] args) {
        VtenSaveCharacterPlaywright app = new VtenSaveCharacterPlaywright();
        app.run(DEFAULT_USER_ID);
    }

    /**
     * Выполняет процесс регистрации аккаунта для указанного пользователя.
     *
     * @param userId идентификатор пользователя Telegram
     */
    public void run(@NotNull Long userId) {
        runInternal(userId, config.isHeadlessBrowser());
    }

    /**
     * Выполняет процесс регистрации аккаунта с явным указанием режима headless.
     *
     * @param userId    идентификатор пользователя Telegram
     * @param headless  признак headless режима браузера
     */
    void runWithHeadless(@NotNull Long userId, boolean headless) {
        runInternal(userId, headless);
    }

    /**
     * Выполняет регистрацию с повторными попытками.
     *
     * @param userId   идентификатор пользователя Telegram
     * @param headless признак headless режима браузера
     */
    private void runInternal(@NotNull Long userId, boolean headless) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < config.getMaxRetries()) {
            attempt++;
            log.info("Попытка {} из {}", attempt, config.getMaxRetries());

            try {
                executeRegistration(userId, headless);
                log.info("Регистрация успешно завершена для userId={}", userId);
                return;
            } catch (PlaywrightException e) {
                lastException = e;
                log.warn("Ошибка Playwright на попытке {}: {}", attempt, e.getMessage());
                if (attempt < config.getMaxRetries()) {
                    try {
                        Thread.sleep(2000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Прервано ожидание перед повторной попыткой", ie);
                        break;
                    }
                }
            } catch (IOException e) {
                lastException = e;
                log.error("Ошибка сохранения данных: {}", e.getMessage(), e);
                break;
            }
        }

        log.error("Не удалось завершить регистрацию после {} попыток для userId={}",
                config.getMaxRetries(), userId, lastException);
    }

    /**
     * Выполняет процесс регистрации с использованием браузера.
     *
     * @param userId идентификатор пользователя Telegram
     * @param headless признак headless режима браузера
     * @throws IOException если не удалось сохранить данные
     */
    private void executeRegistration(@NotNull Long userId, boolean headless) throws IOException {
        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(headless));

            BrowserContext context = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(375, 667));
            Page page = context.newPage();
            page.setDefaultTimeout(config.getPageLoadTimeout());

            try {
                // Шаг 1. Открываем страницу тренировки/выбора героя
                log.debug("Переход на страницу выбора героя");
                page.navigate(config.getTrainingUrl());
                page.waitForSelector(config.getHeroClassSelector());

                // Шаг 2. Переходим на страницу сохранения персонажа
                log.debug("Переход на страницу сохранения персонажа");
                page.navigate(config.getSaveUrl());
                page.waitForSelector(config.getLoginInputSelector());
                page.waitForSelector(config.getPasswordInputSelector());

                // Извлекаем имя персонажа из поля логина
                String heroLogin = page.locator(config.getLoginInputSelector()).inputValue();
                log.debug("Получен логин героя: {}", heroLogin);

                // Генерируем случайный пароль
                String randomPassword = accountService.generatePassword(12);

                // Заполняем пароль
                page.fill(config.getPasswordInputSelector(), randomPassword);

                // Нажимаем кнопку сохранения
                page.click(config.getSaveButtonSelector());

                // Ожидаем завершения перенаправления и загрузки новой страницы
                page.waitForLoadState();

                // Сохраняем учетные данные в JSON
                accountRepository.saveAccount(new Account(userId, heroLogin, randomPassword));
                log.info("Учётные данные сохранены для userId={}, login={}", userId, heroLogin);

            } finally {
                context.close();
                browser.close();
            }
        }
    }
}
