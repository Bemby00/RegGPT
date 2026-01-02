package com.mirteney.model;

import org.jetbrains.annotations.NotNull;

/**
 * Неизменяемая модель аккаунта, содержащая логин, пароль и идентификатор пользователя Telegram.
 *
 * <p>Используется для передачи данных между сервисами и репозиторием.</p>
 */
public record Account(@NotNull Long userId, @NotNull String login, @NotNull String password) {
    /**
     * Компактный конструктор с проверкой валидности данных.
     *
     * @param userId   идентификатор пользователя Telegram
     * @param login    логин аккаунта
     * @param password пароль аккаунта
     */
    public Account {
        if (userId == null || userId <= 0) {
            throw new IllegalArgumentException("ID пользователя должен быть положительным");
        }
        if (login.isBlank()) {
            throw new IllegalArgumentException("Логин не должен быть пустым");
        }
        if (password.isBlank()) {
            throw new IllegalArgumentException("Пароль не должен быть пустым");
        }
    }
}
