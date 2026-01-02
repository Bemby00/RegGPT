package com.mirteney.model;

import org.jetbrains.annotations.NotNull;

/**
 * Нев изменяемая модель аккаунта, содержащая логин и пароль.
 *
 * <p>Используется для передачи данных между сервисами и репозиторием.</p>
 */
public record Account(@NotNull String login, @NotNull String password) {
    /**
     * Компактный конструктор с проверкой валидности данных.
     *
     * @param login    логин аккаунта
     * @param password пароль аккаунта
     */
    public Account {
        if (login.isBlank()) {
            throw new IllegalArgumentException("Логин не должен быть пустым");
        }
        if (password.isBlank()) {
            throw new IllegalArgumentException("Пароль не должен быть пустым");
        }
    }
}
