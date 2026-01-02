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
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Телеграм-бот для генерации аккаунтов и их сохранения в JSON-файл.
 *
 * <p>Класс отвечает только за обработку апдейтов Telegram и делегирует
 * бизнес-логику сервису, а операции ввода/вывода — репозиторию.</p>
 */
public class AccountBot implements LongPollingSingleThreadUpdateConsumer {

    private static final Logger log = LoggerFactory.getLogger(AccountBot.class);
    private static final String GENERATE_COMMAND = "/generate";
    private static final String START_COMMAND = "/start";
    private static final String STATUS_CALLBACK = "status";
    private static final String INVITATION_CALLBACK = "invitation";
    private static final String REGISTER_CALLBACK = "register";
    private static final String LIST_ACCOUNTS_CALLBACK = "list_accounts";
    private static final Pattern INVITATION_PATTERN = Pattern.compile(
            "^https://m\\.vten\\.ru/from/user/\\d+/[A-Za-z0-9]+$");

    private final AccountService accountService;
    private final AccountRepository accountRepository;
    private final TelegramClient telegramClient;
    private final String botToken;
    private final ExecutorService executorService;
    private final Map<Long, Boolean> awaitingInvitation;
    private final Map<Long, String> invitationLinks;

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
        this.executorService = Executors.newSingleThreadExecutor();
        this.awaitingInvitation = new ConcurrentHashMap<>();
        this.invitationLinks = new ConcurrentHashMap<>();
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
        // Обрабатываем callback-запросы от inline-клавиатуры.
        if (update.hasCallbackQuery()) {
            handleCallback(update.getCallbackQuery());
            return;
        }

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

        // Если ожидаем ссылку приглашения, обрабатываем её отдельной веткой.
        if (isAwaitingInvitation(userId)) {
            handleInvitationInput(chatId, userId, userMessage);
            return;
        }

        // Обрабатываем команду стартового меню.
        if (isStartCommand(userMessage)) {
            sendMenu(chatId);
            return;
        }

        // Обрабатываем команду генерации аккаунта.
        if (isGenerateCommand(userMessage)) {
            handleGenerateCommand(chatId, userId);
        }
    }

    /**
     * Обрабатывает callback-запросы от inline-клавиатуры.
     *
     * @param callbackQuery callback-запрос от Telegram
     */
    private void handleCallback(@NotNull CallbackQuery callbackQuery) {
        if (callbackQuery.getData() == null) {
            return;
        }
        if (callbackQuery.getMessage() == null) {
            return;
        }

        String data = callbackQuery.getData().trim().toLowerCase(Locale.ROOT);
        long chatId = callbackQuery.getMessage().getChatId();
        Long userId = callbackQuery.getFrom().getId();

        // Подтверждаем callback, чтобы убрать индикатор загрузки.
        answerCallback(callbackQuery.getId());

        switch (data) {
            case STATUS_CALLBACK -> handleStatus(chatId);
            case INVITATION_CALLBACK -> handleInvitationRequest(chatId, userId);
            case REGISTER_CALLBACK -> handleRegistrationRequest(chatId, userId);
            case LIST_ACCOUNTS_CALLBACK -> handleListAccounts(chatId, userId);
            default -> sendResponse(chatId, "Неизвестная команда. Попробуйте снова.");
        }
    }

    /**
     * Отправляет пользователю статус бота.
     *
     * @param chatId идентификатор чата
     */
    private void handleStatus(long chatId) {
        sendResponse(chatId, "Бот работает в штатном режиме.");
    }

    /**
     * Запрашивает у пользователя ссылку приглашения.
     *
     * @param chatId идентификатор чата
     * @param userId идентификатор пользователя
     */
    private void handleInvitationRequest(long chatId, @NotNull Long userId) {
        awaitingInvitation.put(userId, true);
        sendResponse(chatId, "Отправьте ссылку приглашения формата: https://m.vten.ru/from/user/243360/s8eadv5d");
    }

    /**
     * Обрабатывает полученную ссылку приглашения.
     *
     * @param chatId      идентификатор чата
     * @param userId      идентификатор пользователя
     * @param userMessage сообщение пользователя
     */
    private void handleInvitationInput(long chatId, @NotNull Long userId, @NotNull String userMessage) {
        awaitingInvitation.remove(userId);
        if (!INVITATION_PATTERN.matcher(userMessage).matches()) {
            sendResponse(chatId, "Ссылка не соответствует формату. Попробуйте ещё раз.");
            return;
        }

        invitationLinks.put(userId, userMessage);
        sendResponse(chatId, "Ссылка приглашения сохранена.");
    }

    /**
     * Запускает процесс регистрации аккаунта для пользователя.
     *
     * @param chatId идентификатор чата
     * @param userId идентификатор пользователя
     */
    private void handleRegistrationRequest(long chatId, @NotNull Long userId) {
        sendResponse(chatId, "Запускаю регистрацию аккаунта. Это может занять некоторое время.");

        // Запускаем регистрацию асинхронно, чтобы не блокировать обработчик апдейтов.
        executorService.submit(() -> {
            try {
                VtenSaveCharacterPlaywright registration = new VtenSaveCharacterPlaywright(
                        new AppConfig(),
                        accountService,
                        accountRepository);
                registration.runWithHeadless(userId, true);
                sendResponse(chatId, "Регистрация завершена. Данные аккаунта сохранены.");
            } catch (Exception e) {
                log.error("Ошибка регистрации аккаунта для userId={}", userId, e);
                sendResponse(chatId, "Не удалось завершить регистрацию. Попробуйте позже.");
            }
        });
    }

    /**
     * Возвращает список аккаунтов, привязанных к пользователю.
     *
     * @param chatId идентификатор чата
     * @param userId идентификатор пользователя
     */
    private void handleListAccounts(long chatId, @NotNull Long userId) {
        try {
            List<Account> accounts = accountRepository.findAccountsByUserId(userId);
            if (accounts.isEmpty()) {
                sendResponse(chatId, "У вас нет сохранённых аккаунтов.");
                return;
            }

            StringBuilder responseBuilder = new StringBuilder("Ваши аккаунты:\n");
            for (Account account : accounts) {
                responseBuilder.append("- ").append(account.login()).append("\n");
            }
            sendResponse(chatId, responseBuilder.toString());
        } catch (IOException e) {
            log.error("Не удалось загрузить аккаунты для userId={}", userId, e);
            sendResponse(chatId, "Ошибка при загрузке аккаунтов. Попробуйте позже.");
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
     * Отправляет inline-меню с основными действиями.
     *
     * @param chatId идентификатор чата
     */
    private void sendMenu(long chatId) {
        InlineKeyboardMarkup markup = InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        new InlineKeyboardRow(List.of(
                                InlineKeyboardButton.builder()
                                        .text("Статус бота")
                                        .callbackData(STATUS_CALLBACK)
                                        .build())),
                        new InlineKeyboardRow(List.of(
                                InlineKeyboardButton.builder()
                                        .text("Приглашение")
                                        .callbackData(INVITATION_CALLBACK)
                                        .build())),
                        new InlineKeyboardRow(List.of(
                                InlineKeyboardButton.builder()
                                        .text("Регистрация аккаунта")
                                        .callbackData(REGISTER_CALLBACK)
                                        .build())),
                        new InlineKeyboardRow(List.of(
                                InlineKeyboardButton.builder()
                                        .text("Список аккаунтов")
                                        .callbackData(LIST_ACCOUNTS_CALLBACK)
                                        .build()))
                ))
                .build();

        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text("Выберите действие:")
                .replyMarkup(markup)
                .build();

        try {
            telegramClient.execute(message);
        } catch (TelegramApiException e) {
            log.error("Не удалось отправить меню в чат {}", chatId, e);
        }
    }

    /**
     * Подтверждает получение callback-запроса.
     *
     * @param callbackId идентификатор callback-запроса
     */
    private void answerCallback(@NotNull String callbackId) {
        AnswerCallbackQuery answer = AnswerCallbackQuery.builder()
                .callbackQueryId(callbackId)
                .build();
        try {
            telegramClient.execute(answer);
        } catch (TelegramApiException e) {
            log.warn("Не удалось подтвердить callback {}", callbackId, e);
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
     * Проверяет, является ли сообщение командой запуска меню.
     *
     * @param userMessage текст сообщения
     * @return true, если команда совпадает
     */
    private boolean isStartCommand(@NotNull String userMessage) {
        return userMessage.toLowerCase(Locale.ROOT).equals(START_COMMAND);
    }

    /**
     * Проверяет, ожидается ли ссылка приглашения от пользователя.
     *
     * @param userId идентификатор пользователя
     * @return true, если ожидается ссылка
     */
    private boolean isAwaitingInvitation(@NotNull Long userId) {
        return Boolean.TRUE.equals(awaitingInvitation.get(userId));
    }
}
