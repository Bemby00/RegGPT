# RegGPT - Автоматизация регистрации аккаунтов

Проект для автоматической регистрации аккаунтов на m.vten.ru с использованием Playwright и Telegram Bot API 9.2.

## Быстрый старт

1. Скопируйте `config.properties.example` в `config.properties`
2. Укажите токен бота в `config.properties`:
   ```properties
   telegram.bot.token=YOUR_BOT_TOKEN
   encryption.key=YOUR_ENCRYPTION_KEY
   ```
3. Запустите бота (потребуется создать main-класс)
4. Отправьте `/generate` боту в Telegram

## Возможности

- Автоматическая генерация уникальных логинов на основе UUID
- Генерация безопасных паролей с использованием SecureRandom
- Поддержка нескольких пользователей через Telegram бота
- Шифрование паролей в файле хранения (AES-GCM)
- Защита от race conditions при конкурентной записи
- Механизм повторных попыток при ошибках браузера
- Настройка через переменные окружения

## Требования

- Java 21+
- Maven 3.6+
- Chromium (для Playwright)

## Установка

1. Клонируйте репозиторий:
```bash
git clone <repository-url>
cd RegGPT
```

2. Установите зависимости:
```bash
mvn clean install
```

3. Установите браузеры для Playwright:
```bash
mvn exec:java -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args="install chromium"
```

## Конфигурация

### Способ 1: Файл конфигурации (рекомендуется)

1. Скопируйте файл-пример:
```bash
cp config.properties.example config.properties
```

2. Отредактируйте `config.properties`:
```properties
# Обязательные настройки
telegram.bot.token=YOUR_BOT_TOKEN_HERE
encryption.key=YOUR_ENCRYPTION_KEY_HERE

# Настройки Playwright (опционально)
vten.headless=false
vten.page.timeout=30000
vten.max.retries=3
```

**Важно**: Файл `config.properties` автоматически добавлен в `.gitignore` и не будет закоммичен.

### Способ 2: Переменные окружения (альтернатива)

Если файл `config.properties` не найден, используются переменные окружения:

```bash
export TELEGRAM_BOT_TOKEN="your-bot-token"
export ENCRYPTION_KEY="your-strong-encryption-password"
export VTEN_HEADLESS="false"
export VTEN_PAGE_TIMEOUT="30000"
export VTEN_MAX_RETRIES="3"
```

### Приоритет загрузки настроек:
1. Файл `config.properties` (если существует)
2. Переменные окружения
3. Значения по умолчанию

## Использование

### Telegram бот

Запустите бота (требуется создать точку входа):
```bash
mvn exec:java -D exec.mainClass=com.mirteney.BotMain
```

Отправьте команду боту в Telegram:
```
/generate
```

Бот создаст новый аккаунт и отправит вам логин и пароль.

### Standalone режим

Запустите прямую регистрацию:
```bash
mvn exec:java -D exec.mainClass=com.mirteney.VtenSaveCharacterPlaywright
```

## Архитектура

### Основные компоненты:

- **Account** - модель данных аккаунта (userId, login, password)
- **AccountService** - бизнес-логика генерации аккаунтов
- **AccountRepository** - сохранение/загрузка аккаунтов с шифрованием
- **PasswordGenerator** - генерация безопасных паролей
- **PasswordEncryption** - шифрование паролей с использованием AES-GCM
- **AccountBot** - Telegram бот для взаимодействия с пользователями
- **VtenSaveCharacterPlaywright** - автоматизация регистрации через браузер
- **AppConfig** - централизованная конфигурация

### Безопасность:

1. **Шифрование паролей**: AES-GCM с 256-битным ключом
2. **Защита от race conditions**: Использование файловых блокировок
3. **Безопасная генерация паролей**: SecureRandom
4. **Валидация входных данных**: Проверка длины паролей (макс. 128 символов)
5. **Мультипользовательская поддержка**: Привязка аккаунтов к userId

## Обновления в версии 2.0

### Исправлены ошибки:
- ✅ Исправлена race condition в AccountRepository
- ✅ Исправлена опечатка в комментариях
- ✅ Убрано дублирование экземпляров SecureRandom
- ✅ Добавлен accounts.json в .gitignore

### Улучшения:
- ✅ Обновлен Telegram Bots API до версии 9.3
- ✅ Обновлены все зависимости до актуальных версий
- ✅ Генерация логинов на основе UUID вместо случайных чисел
- ✅ Добавлена валидация максимальной длины пароля
- ✅ Добавлена мультипользовательская поддержка (userId)
- ✅ Добавлено шифрование паролей
- ✅ Добавлен механизм retry для Playwright
- ✅ Вынесены hardcoded значения в конфигурацию
- ✅ Улучшена обработка ошибок

## Структура хранения данных

Аккаунты сохраняются в `accounts.json`:
```json
[
  {
    "userId": 123456789,
    "login": "User1a2b3c4d",
    "password": "зашифрованный_пароль_в_base64"
  }
]
```

При отключенном шифровании пароли сохраняются в открытом виде.

## Тестирование

Запустите тесты:
```bash
mvn test
```

## Лицензия

Этот проект предназначен только для образовательных целей.

## Важные замечания

1. **Безопасность**: Всегда используйте ENCRYPTION_KEY в production
2. **Git**: Файл accounts.json добавлен в .gitignore
3. **История Git**: Если accounts.json уже был закоммичен, удалите его из истории:
   ```bash
   git filter-branch --force --index-filter \
   "git rm --cached --ignore-unmatch accounts.json" \
   --prune-empty --tag-name-filter cat -- --all
   ```

## Поддержка

При возникновении проблем проверьте:
1. Правильность установки переменных окружения
2. Доступность браузера Chromium
3. Корректность токена Telegram бота
4. Права доступа к файлу accounts.json
