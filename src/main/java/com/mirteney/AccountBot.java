package com.mirteney;

import com.mirteney.config.AppConfig;
import com.mirteney.model.Account;
import com.mirteney.repository.AccountRepository;
import com.mirteney.service.AccountService;
import com.mirteney.service.PasswordGenerator;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Телеграм-бот для генерации аккаунтов и их сохранения в JSON-файл.
 *
 * <p>Класс отвечает только за обработку апдейтов Telegram и делегирует
 * бизнес-логику сервису, а операции ввода/вывода — репозиторию.</p>
 */
public class AccountBot implements LongPollingSingleThreadUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(AccountBot.class);
    private static final String GENERATE_COMMAND = "/generate";

    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final TelegramClient telegramClient;
    private final String botToken;

    /**
     * Создаёт бота с зависимостями по умолчанию и загрузкой настроек из config.properties.
     *
     * @param telegramClient клиент Telegram API
     */
    public AccountBot(@NotNull TelegramClient telegramClient) {
        this(telegramClient, new AppConfig());
    }

    /**
     * Создаёт бота с зависимостями по умолчанию и указанной конфигурацией.
     *
     * @param telegramClient клиент Telegram API
     * @param config         конфигурация приложения
     */
    public AccountBot(@NotNull TelegramClient telegramClient,
                      @NotNull AppConfig config) {
        this(telegramClient,
                new AccountService(new PasswordGenerator()),
                new AccountRepository(Path.of("accounts.json")),
                config.getBotToken());
    }

    /**
     * Создаёт бота с внедрением зависимостей.
     *
     * @param telegramClient    клиент Telegram API
     * @param accountService    сервис генерации аккаунтов
     * @param accountRepository репозиторий для сохранения данных
     * @param botToken          токен бота
     */
    public AccountBot(@NotNull TelegramClient telegramClient,
                      @NotNull AccountService accountService,
                      @NotNull AccountRepository accountRepository,
                      @NotNull String botToken) {
        this.telegramClient = telegramClient;
        this.accountService = accountService;
        this.accountRepository = accountRepository;
        this.botToken = botToken;
    }

    /**
     * Возвращает токен бота.
     *
     * @return токен бота
     */
    public String getBotToken() {
        return botToken;
    }

    /**
     * Обрабатывает входящие сообщения и выполняет команды.
     *
     * @param update обновление Telegram
     */
    @Override
    public void consume(@NotNull Update update) {
        // Проверяем наличие сообщения, чтобы избежать NullPointerException.
        if (!update.hasMessage()) {
            return;
        }

        Message message = update.getMessage();
        if (message == null || message.getText() == null || message.getText().isBlank()) {
            return;
        }

        String userMessage = message.getText().trim();
        long chatId = message.getChatId();
        Long userId = message.getFrom().getId();

        // Обрабатываем команду генерации аккаунта.
        if (isGenerateCommand(userMessage)) {
            handleGenerateCommand(chatId, userId);
        }
    }

    /**
     * Обрабатывает команду генерации аккаунта и отправляет результат пользователю.
     *
     * @param chatId идентификатор чата
     * @param userId идентификатор пользователя
     */
    private void handleGenerateCommand(long chatId, @NotNull Long userId) {
        try {
            Account account = accountService.createAccount(userId);
            accountRepository.saveAccount(account);

            // Формируем ответ без логирования чувствительных данных.
            String responseText = "Новый аккаунт сгенерирован!\n" +
                    "Логин: " + account.login() + "\n" +
                    "Пароль: " + account.password();
            sendResponse(chatId, responseText);
            log.info("Аккаунт создан для userId={}", userId);
        } catch (IOException e) {
            log.error("Не удалось сохранить аккаунт для userId={}, chatId={}", userId, chatId, e);
            sendResponse(chatId, "Ошибка при сохранении аккаунта. Попробуйте позже.");
        } catch (Exception e) {
            log.error("Неожиданная ошибка при генерации аккаунта для userId={}", userId, e);
            sendResponse(chatId, "Произошла ошибка. Попробуйте позже.");
        }
    }

    /**
     * Отправляет текстовое сообщение пользователю.
     *
     * @param chatId идентификатор чата
     * @param text   текст ответа
     */
    private void sendResponse(long chatId, @NotNull String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        try {
            telegramClient.execute(message);
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
}
