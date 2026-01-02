package com.mirteney.service;

import com.mirteney.model.Account;
import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;

/**
 * Сервис бизнес-логики для генерации аккаунтов.
 *
 * <p>Отвечает за создание логина и пароля, не выполняя операций ввода/вывода.</p>
 */
public class AccountService {

    private static final String LOGIN_PREFIX = "User";
    private static final int DEFAULT_PASSWORD_LENGTH = 12;

    private final SecureRandom secureRandom = new SecureRandom();
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
     * Генерирует новый аккаунт со стандартной длиной пароля.
     *
     * @return новый аккаунт
     */
    public @NotNull Account createAccount() {
        String login = generateLogin();
        String password = generatePassword(DEFAULT_PASSWORD_LENGTH);
        return new Account(login, password);
    }

    /**
     * Генерирует логин с префиксом и случайным числом.
     *
     * @return логин
     */
    public @NotNull String generateLogin() {
        int suffix = secureRandom.nextInt(1000);
        return LOGIN_PREFIX + suffix;
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
