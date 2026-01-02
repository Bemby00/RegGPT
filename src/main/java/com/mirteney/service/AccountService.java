package com.mirteney.service;

import com.mirteney.model.Account;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Сервис бизнес-логики для генерации аккаунтов.
 *
 * <p>Отвечает за создание логина и пароля, не выполняя операций ввода/вывода.</p>
 */
public class AccountService {

    private static final String LOGIN_PREFIX = "User";
    private static final int DEFAULT_PASSWORD_LENGTH = 12;

    private final PasswordGenerator passwordGenerator;

    /**
     * Создаёт сервис с заданным генератором паролей.
     *
     * @param passwordGenerator генератор паролей
     */
    public AccountService(@NotNull PasswordGenerator passwordGenerator) {
        this.passwordGenerator = passwordGenerator;
    }

    /**
     * Генерирует новый аккаунт со стандартной длиной пароля для указанного пользователя.
     *
     * @param userId идентификатор пользователя Telegram
     * @return новый аккаунт
     */
    public @NotNull Account createAccount(@NotNull Long userId) {
        String login = generateLogin();
        String password = generatePassword(DEFAULT_PASSWORD_LENGTH);
        return new Account(userId, login, password);
    }

    /**
     * Генерирует уникальный логин на основе UUID.
     *
     * @return логин
     */
    public @NotNull String generateLogin() {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return LOGIN_PREFIX + uuid.substring(0, 8);
    }

    /**
     * Генерирует пароль заданной длины.
     *
     * @param length длина пароля
     * @return пароль
     */
    public @NotNull String generatePassword(int length) {
        return passwordGenerator.generate(length);
    }
}
