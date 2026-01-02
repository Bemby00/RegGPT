package com.mirteney;

import com.mirteney.config.AppConfig;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.concurrent.CountDownLatch;

/**
 * Главная точка входа для запуска Telegram-бота в режиме long polling.
 *
 * <p>Класс отвечает за создание конфигурации, инициализацию клиента Telegram,
 * регистрацию бота и удержание процесса активным до завершения работы.</p>
 */
public class BotMain {

    private static final Logger log = LoggerFactory.getLogger(BotMain.class);
    private static final String SKIP_START_PROPERTY = "bot.main.skip";

    /**
     * Точка входа приложения.
     *
     * @param args аргументы командной строки
     */
    public static void main(@NotNull String[] args) {
        // В тестовой среде можно отключить запуск, установив системное свойство.
        if (Boolean.getBoolean(SKIP_START_PROPERTY)) {
            log.warn("Запуск бота пропущен из-за свойства {}", SKIP_START_PROPERTY);
            return;
        }

        BotMain app = new BotMain();
        app.launch();
    }

    /**
     * Запускает бот, регистрирует его в long polling приложении и ожидает завершения.
     */
    private void launch() {
        AppConfig config = new AppConfig();
        String botToken = config.getBotToken();
        TelegramClient telegramClient = new OkHttpTelegramClient(botToken);

        try (TelegramBotsLongPollingApplication application = new TelegramBotsLongPollingApplication()) {
            // Регистрируем обработчик обновлений.
            application.registerBot(botToken, new AccountBot(telegramClient, config));
            log.info("Бот запущен и ожидает обновления.");

            // Блокируем поток, чтобы приложение не завершилось.
            waitIndefinitely();
        } catch (TelegramApiException e) {
            log.error("Не удалось запустить Telegram-бота", e);
        } catch (Exception e) {
            log.error("Неожиданная ошибка при запуске Telegram-бота", e);
        }
    }

    /**
     * Блокирует текущий поток до прерывания, чтобы приложение оставалось активным.
     */
    private void waitIndefinitely() {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Ожидание остановлено из-за прерывания потока", e);
        }
    }
}
