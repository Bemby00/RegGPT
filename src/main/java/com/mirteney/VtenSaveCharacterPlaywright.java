package com.mirteney;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
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
 * Пример автоматизации процесса выбора героя и сохранения персонажа с помощью Playwright.
 *
 * <p>Программа открывает страницу выбора класса героя на m.vten.ru, ждёт её полной
 * загрузки, затем переходит на страницу сохранения персонажа, заполняет поле
 * пароля и нажимает кнопку сохранения.</p>
 */
public class VtenSaveCharacterPlaywright {

    private static final Logger log = LoggerFactory.getLogger(VtenSaveCharacterPlaywright.class);

    /**
     * Точка входа приложения с демонстрацией автоматизации.
     *
     * @param args аргументы командной строки
     */
    public static void main(@NotNull String[] args) {
        AccountService accountService = new AccountService(new PasswordGenerator());
        AccountRepository accountRepository = new AccountRepository(Path.of("accounts.json"));

        // Запускаем Playwright.
        try (Playwright playwright = Playwright.create()) {
            // Открываем браузер (можно включить headless=true для работы без UI).
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(false));
            Page page = browser.newPage();

            // Шаг 1. Открываем страницу тренировки/выбора героя.
            page.navigate("https://m.vten.ru/training/battle");
            // Ждём появления элемента с заголовком выбора класса героя.
            page.waitForSelector("text=ВЫБЕРИ КЛАСС ГЕРОЯ");

            // Шаг 2. Переходим на страницу сохранения персонажа.
            page.navigate("https://m.vten.ru/user/save");
            // Ждём загрузки формы (поля логина и пароля).
            page.waitForSelector("input[name='loginContainer:name']");
            page.waitForSelector("input[name='passwordContainer:password']");

            // Извлекаем имя персонажа из поля loginContainer:name.
            String heroLogin = page.locator("input[name='loginContainer:name']").inputValue();

            // Генерируем случайный пароль.
            String randomPassword = accountService.generatePassword(12);

            // Заполняем пароль.
            page.fill("input[name='passwordContainer:password']", randomPassword);

            // Нажимаем кнопку сохранения.
            page.click("button.btn-rich3._main");

            // Ожидаем завершения перенаправления и загрузки новой страницы.
            page.waitForLoadState();

            // Сохраняем учетные данные в JSON (без логирования пароля).
            try {
                accountRepository.saveAccount(new Account(heroLogin, randomPassword));
                log.info("Учётные данные сохранены: login={}", heroLogin);
            } catch (IOException e) {
                log.error("Не удалось сохранить учётные данные: login={}", heroLogin, e);
            }

            log.info("Сохранение персонажа завершено.");

            // Закрываем браузер.
            browser.close();
        }
    }
}
