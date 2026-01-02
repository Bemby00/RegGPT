package com.mirteney.service;

import org.jetbrains.annotations.NotNull;

import java.security.SecureRandom;

/**
 * Генератор случайных паролей на основе SecureRandom.
 *
 * <p>Используется для безопасного создания паролей заданной длины.</p>
 */
public class PasswordGenerator {

    private static final String PASSWORD_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_PASSWORD_LENGTH = 128;

    /**
     * Генерирует случайный пароль указанной длины.
     *
     * @param length длина пароля
     * @return случайный пароль
     */
    public @NotNull String generate(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Длина пароля должна быть положительной");
        }
        if (length > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Длина пароля не должна превышать " + MAX_PASSWORD_LENGTH);
        }

        StringBuilder password = new StringBuilder(length);

        // Формируем строку из случайных символов допустимого набора.
        for (int i = 0; i < length; i++) {
            int index = SECURE_RANDOM.nextInt(PASSWORD_CHARS.length());
            password.append(PASSWORD_CHARS.charAt(index));
        }
        return password.toString();
    }
}
