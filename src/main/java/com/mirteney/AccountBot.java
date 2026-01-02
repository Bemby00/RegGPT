package com.mirteney;

import com.mirteney.model.Account;
import com.mirteney.repository.AccountRepository;
import com.mirteney.service.AccountService;
import com.mirteney.service.PasswordGenerator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/**
 * Телеграм-бот для генерации аккаунтов и их сохранения в JSON-файл.
 *
 * <p>Класс отвечает только за обработку апдейтов Telegram и делегирует
 * бизнес-логику сервису, а операции ввода/вывода — репозиторию.</p>
 */
public class AccountBot extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(AccountBot.class);
    private static final String GENERATE_COMMAND = "/generate";

    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final String botUsername;
    private final String botToken;

    /**
     * Создаёт бота с зависимостями по умолчанию и загрузкой настроек из окружения.
     */
    public AccountBot() {
        this(new AccountService(new PasswordGenerator()),
                new AccountRepository(Path.of("accounts.json")),
                resolveEnv("TELEGRAM_BOT_USERNAME"),
                resolveEnv("TELEGRAM_BOT_TOKEN"));
    }

    /**
     * Создаёт бота с внедрением зависимостей.
     *
     * @param accountService    сервис генерации аккаунтов
     * @param accountRepository репозиторий для сохранения данных
     * @param botUsername       имя бота
     * @param botToken          токен бота
     */
    public AccountBot(@NotNull AccountService accountService,
                      @NotNull AccountRepository accountRepository,
                      @NotNull String botUsername,
                      @NotNull String botToken) {
        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.botUsername = botUsername;
        this.botToken = botToken;
    }

    /**
     * Возвращает имя бота, загруженное из конфигурации.
     *
     * @return имя бота
     */
    @Override
    public String getBotUsername() {
        return botUsername;
    }

    /**
     * Возвращает токен бота, загруженный из конфигурации.
     *
     * @return токен бота
     */
    @Override
    public String getBotToken() {
        return botToken;
    }

    /**
     * Обрабатывает входящие сообщения и выполняет команды.
     *
     * @param update обновление Telegram
     */
    @Override
    public void onUpdateReceived(@NotNull Update update) {
        // Проверяем наличие сообщения, чтобы избежать NullPointerException.
        if (!update.hasMessage()) {
            return;
        }

        Message message = update.getMessage();
        if (message == null || !message.hasText()) {
            return;
        }

        String userMessage = message.getText().trim();
        long chatId = message.getChatId();

        // Обрабатываем команду генерации аккаунта.
        if (isGenerateCommand(userMessage)) {
            handleGenerateCommand(chatId);
        }
    }

    /**
     * Обрабатывает команду генерации аккаунта и отправляет результат пользователю.
     *
     * @param chatId идентификатор чата
     */
    private void handleGenerateCommand(long chatId) {
        try {
            Account account = accountService.createAccount();
            accountRepository.saveAccount(account);

            // Формируем ответ без логирования чувствительных данных.
            String responseText = "Новый аккаунт сгенерирован!\n" +
                    "Логин: " + account.login() + "\n" +
                    "Пароль: " + account.password();
            sendResponse(chatId, responseText);
        } catch (IOException e) {
            log.error("Не удалось сохранить аккаунт для chatId={}", chatId, e);
            sendResponse(chatId, "Ошибка при сохранении аккаунта. Попробуйте позже.");
        }
    }

    /**
     * Отправляет текстовое сообщение пользователю.
     *
     * @param chatId идентификатор чата
     * @param text   текст ответа
     */
    private void sendResponse(long chatId, @NotNull String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Не удалось отправить сообщение в чат {}", chatId, e);
        }
    }

    /**
     * Проверяет, является ли сообщение командой генерации.
     *
     * @param userMessage текст сообщения
     * @return true, если команда совпадает
     */
    private boolean isGenerateCommand(@NotNull String userMessage) {
        return userMessage.toLowerCase(Locale.ROOT).equals(GENERATE_COMMAND);
    }

    /**
     * Получает значение переменной окружения или бросает исключение.
     *
     * @param envName имя переменной окружения
     * @return значение переменной окружения
     */
    private static String resolveEnv(@NotNull String envName) {
        return Optional.ofNullable(System.getenv(envName))
                .filter(value -> !value.isBlank())
                .orElseThrow(() -> new IllegalStateException(
                        "Не задана переменная окружения: " + envName));
    }
}
