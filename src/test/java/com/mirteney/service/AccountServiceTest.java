package com.mirteney.service;

import com.mirteney.model.Account;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Юнит-тесты сервиса генерации аккаунтов.
 */
class AccountServiceTest {

    /**
     * Проверяет, что логин генерируется с ожидаемым префиксом.
     */
    @Test
    void generateLogin_returnsLoginWithPrefix() {
        // given
        AccountService service = new AccountService(new PasswordGenerator());

        // when
        String login = service.generateLogin();

        // then
        assertNotNull(login);
        assertTrue(login.startsWith("User"));
    }

    /**
     * Проверяет, что пароль генерируется корректной длины.
     */
    @Test
    void generatePassword_returnsPasswordWithExpectedLength() {
        // given
        AccountService service = new AccountService(new PasswordGenerator());

        // when
        String password = service.generatePassword(10);

        // then
        assertNotNull(password);
        assertEquals(10, password.length());
    }

    /**
     * Проверяет, что создаваемый аккаунт содержит логин и пароль.
     */
    @Test
    void createAccount_returnsAccountWithLoginAndPassword() {
        // given
        AccountService service = new AccountService(new PasswordGenerator());

        // when
        Account account = service.createAccount();

        // then
        assertNotNull(account);
        assertTrue(account.login().startsWith("User"));
        assertEquals(12, account.password().length());
    }
}
