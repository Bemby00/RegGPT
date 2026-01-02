package com.mirteney;

import org.junit.jupiter.api.Test;

/**
 * Тесты для проверки поведения точки входа BotMain.
 */
class BotMainTest {

    /**
     * Проверяет, что запуск можно безопасно пропустить через системное свойство.
     */
    @Test
    void mainSkipsLaunchWhenPropertyEnabled() {
        System.setProperty("bot.main.skip", "true");
        try {
            BotMain.main(new String[0]);
        } finally {
            System.clearProperty("bot.main.skip");
        }
    }
}
