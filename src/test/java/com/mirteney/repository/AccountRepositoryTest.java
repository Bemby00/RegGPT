package com.mirteney.repository;

import com.mirteney.model.Account;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Юнит-тесты репозитория аккаунтов.
 */
class AccountRepositoryTest {

    /**
     * Проверяет, что при отсутствии файла возвращается пустой список.
     */
    @Test
    void loadAccounts_returnsEmptyListWhenFileMissing(@TempDir Path tempDir) throws IOException {
        // given
        Path filePath = tempDir.resolve("accounts.json");
        AccountRepository repository = new AccountRepository(filePath);

        // when
        List<Account> accounts = repository.loadAccounts();

        // then
        assertEquals(0, accounts.size());
    }

    /**
     * Проверяет, что сохранённый аккаунт доступен для чтения.
     */
    @Test
    void saveAccount_persistsAccount(@TempDir Path tempDir) throws IOException {
        // given
        Path filePath = tempDir.resolve("accounts.json");
        AccountRepository repository = new AccountRepository(filePath);
        Account account = new Account(1L, "User123", "Password1!");

        // when
        repository.saveAccount(account);
        List<Account> accounts = repository.loadAccounts();

        // then
        assertEquals(1, accounts.size());
        assertEquals("User123", accounts.get(0).login());
    }

    /**
     * Проверяет, что список аккаунтов неизменяемый.
     */
    @Test
    void loadAccounts_returnsUnmodifiableList(@TempDir Path tempDir) throws IOException {
        // given
        Path filePath = tempDir.resolve("accounts.json");
        AccountRepository repository = new AccountRepository(filePath);
        repository.saveAccount(new Account(1L, "User1", "Password1!"));

        // when
        List<Account> accounts = repository.loadAccounts();

        // then
        assertThrows(UnsupportedOperationException.class,
                () -> accounts.add(new Account(2L, "User2", "Password2!")));
    }
}
