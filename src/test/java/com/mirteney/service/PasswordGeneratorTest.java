package com.mirteney.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Юнит-тесты генератора паролей.
 */
class PasswordGeneratorTest {

    /**
     * Проверяет, что генерируемый пароль имеет нужную длину и не null.
     */
    @Test
    void generate_returnsPasswordWithRequestedLength() {
        // given
        PasswordGenerator generator = new PasswordGenerator();
        int length = 16;

        // when
        String password = generator.generate(length);

        // then
        assertNotNull(password);
        assertEquals(length, password.length());
    }

    /**
     * Проверяет, что генератор отклоняет некорректную длину.
     */
    @Test
    void generate_throwsExceptionOnInvalidLength() {
        // given
        PasswordGenerator generator = new PasswordGenerator();

        // when/then
        assertThrows(IllegalArgumentException.class, () -> generator.generate(0));
    }
}
