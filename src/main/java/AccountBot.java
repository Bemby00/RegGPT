package com.mirteney;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.util.*;
import org.json.*;

public class AccountBot extends TelegramLongPollingBot {

    private static final String BOT_USERNAME = "YourBotUsername"; // Замените на имя вашего бота
    private static final String BOT_TOKEN = "YourBotToken"; // Замените на токен вашего бота

    @Override
    public String getBotUsername() {
        return BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();

        if (message != null && message.hasText()) {
            String userMessage = message.getText();
            long chatId = message.getChatId();

            // Пример команды для генерации аккаунта
            if (userMessage.equalsIgnoreCase("/generate")) {
                String login = "User" + new Random().nextInt(1000);
                String password = generateRandomPassword();

                // Сохранение аккаунта в JSON
                saveAccountToJson(login, password);

                String responseText = "Новый аккаунт сгенерирован!\nЛогин: " + login + "\nПароль: " + password;
                sendResponse(chatId, responseText);
            }
        }
    }

    // Метод для отправки ответа в Telegram
    private void sendResponse(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Генерация случайного пароля
    private String generateRandomPassword() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*";
        StringBuilder password = new StringBuilder();
        Random random = new Random();

        for (int i = 0; i < 10; i++) {
            int index = random.nextInt(chars.length());
            password.append(chars.charAt(index));
        }
        return password.toString();
    }

    // Сохранение аккаунта в JSON файл
    private void saveAccountToJson(String login, String password) {
        JSONObject accountJson = new JSONObject();
        accountJson.put("login", login);
        accountJson.put("password", password);

        try (FileWriter file = new FileWriter("accounts.json", true)) {
            file.write(accountJson.toString() + "\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}