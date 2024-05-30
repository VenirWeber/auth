package ru.telegram_auth;

import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main {
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            try {
                botsApi.registerBot(new NGB());
            } catch (TelegramApiException e) {
                System.err.println("Error removing old webhook: " + e.getMessage());
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}