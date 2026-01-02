import com.microsoft.playwright.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Пример автоматизации процесса выбора героя и сохранения персонажа с помощью Playwright.
 *
 * <p>Программа открывает страницу выбора класса героя на m.vten.ru, ждёт её полной
 * загрузки, затем переходит на страницу сохранения персонажа, заполняет поле
 * пароля и нажимает кнопку сохранения. При необходимости параметры селекторов
 * и URL можно подстроить под актуальную версию сайта.</p>
 */
public class VtenSaveCharacterPlaywright {
    public static void main(String[] args) {
        // Запускаем Playwright
        try (Playwright playwright = Playwright.create()) {
            // Открываем браузер (можно включить headless=true для работы без UI)
            Browser browser = playwright.chromium().launch(
                    new BrowserType.LaunchOptions().setHeadless(false));
            Page page = browser.newPage();

            // Шаг 1. Открываем страницу тренировки/выбора героя
            page.navigate("https://m.vten.ru/training/battle");
            // Ждём появления элемента с заголовком выбора класса героя
            page.waitForSelector("text=ВЫБЕРИ КЛАСС ГЕРОЯ");

            // Шаг 2. Переходим на страницу сохранения персонажа
            page.navigate("https://m.vten.ru/user/save");
            // Ждём загрузки формы (поля логина и пароля)
            page.waitForSelector("input[name='loginContainer:name']");
            page.waitForSelector("input[name='passwordContainer:password']");

            // Извлекаем имя персонажа из поля loginContainer:name
            String heroLogin = page.locator("input[name='loginContainer:name']").inputValue();

            // Генерируем случайный пароль
            String randomPassword = generateRandomPassword(12);

            // Заполняем пароль
            page.fill("input[name='passwordContainer:password']", randomPassword);

            // Нажимаем кнопку сохранения
            page.click("button.btn-rich3._main");

            // Ожидаем завершения перенаправления и загрузки новой страницы
            page.waitForLoadState();

            // Сохраняем учетные данные в JSON
            try {
                saveCredentials(heroLogin, randomPassword);
                System.out.println("Учётные данные сохранены в accounts.json: " + heroLogin);
            } catch (IOException e) {
                System.err.println("Не удалось сохранить учётные данные: " + e.getMessage());
            }

            System.out.println("Сохранение персонажа завершено.");

            // Закрываем браузер
            browser.close();
        }
    }

    // Набор символов для генерации пароля
    private static final String PASSWORD_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Генерирует случайный пароль указанной длины.
     * @param length длина пароля
     * @return случайный пароль
     */
    private static String generateRandomPassword(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int idx = SECURE_RANDOM.nextInt(PASSWORD_CHARS.length());
            sb.append(PASSWORD_CHARS.charAt(idx));
        }
        return sb.toString();
    }

    /**
     * Сохраняет логин и пароль в JSON файл accounts.json. Если файл существует,
     * данные добавляются в конец массива. Если файл отсутствует, создаётся
     * новый файл с одним элементом.
     * @param login логин персонажа
     * @param password пароль персонажа
     */
    private static void saveCredentials(String login, String password) throws IOException {
        Path filePath = Paths.get("accounts.json");
        ObjectMapper mapper = new ObjectMapper();
        List<Map<String, String>> accounts;
        if (Files.exists(filePath)) {
            // Читаем существующий массив аккаунтов
            byte[] jsonBytes = Files.readAllBytes(filePath);
            if (jsonBytes.length > 0) {
                accounts = mapper.readValue(jsonBytes,
                        new TypeReference<List<Map<String, String>>>() {});
            } else {
                accounts = new ArrayList<>();
            }
        } else {
            accounts = new ArrayList<>();
        }
        // Добавляем новую запись
        Map<String, String> record = new HashMap<>();
        record.put("login", login);
        record.put("password", password);
        accounts.add(record);
        // Записываем обратно с отступами
        mapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), accounts);
    }
}