package ru.telegram_auth;

import org.hibernate.Session;
import org.hibernate.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

public class NGB extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(NGB.class);
    private final Map<Long, UserState> userStates = new HashMap<>();

    private enum State {
        CHOOSE_ROLE, ENTER_USERNAME, ENTER_PASSWORD
    }

    private static class UserState {
        State state;
        String role;
        String username;
        String password;
    }

    @Override
    public String getBotUsername() {
        return "never_gonna_bot";
    }

    @Override
    public String getBotToken() {
        return "7440094103:AAH0sl_6E2MGhvGo2FvNIcoZp4ggsHGun3U";
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleTextMessage(update.getMessage());
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery().getMessage(), update.getCallbackQuery().getData());
        }
    }

    private void handleTextMessage(Message message) {
        long chatId = message.getChatId();
        String text = message.getText();

        if ("/start".equals(text)) {
            sendRoleSelectionMessage(chatId);
        } else {
            UserState userState = userStates.get(chatId);
            if (userState != null) {
                switch (userState.state) {
                    case ENTER_USERNAME:
                        userState.username = text;
                        userState.state = State.ENTER_PASSWORD;
                        sendPasswordMessage(chatId);
                        break;
                    case ENTER_PASSWORD:
                        userState.password = text;
                        processRegistrationOrLogin(chatId, userState);
                        break;
                    default:
                        sendUnknownCommandMessage(chatId);
                }
            } else {
                sendUnknownCommandMessage(chatId);
            }
        }
    }

    private void handleCallbackQuery(Message message, String data) {
        long chatId = message.getChatId();
        UserState userState = userStates.getOrDefault(chatId, new UserState());

        switch (data) {
            case "register_student":
                userState.role = "student";
                userState.state = State.ENTER_USERNAME;
                sendUsernameMessage(chatId);
                break;
            case "register_teacher":
                userState.role = "teacher";
                userState.state = State.ENTER_USERNAME;
                sendUsernameMessage(chatId);
                break;
            case "login_student":
                userState.role = "student";
                userState.state = State.ENTER_USERNAME;
                sendUsernameMessage(chatId);
                break;
            case "login_teacher":
                userState.role = "teacher";
                userState.state = State.ENTER_USERNAME;
                sendUsernameMessage(chatId);
                break;
            default:
                sendUnknownCommandMessage(chatId);
        }

        userStates.put(chatId, userState);
        deleteMessage(message);
    }

    private void sendRoleSelectionMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Выберите свою роль:");

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        rowInline.add(InlineKeyboardButton.builder().text("Регистрация студента").callbackData("register_student").build());
        rowInline.add(InlineKeyboardButton.builder().text("Регистрация преподавателя").callbackData("register_teacher").build());
        rowsInline.add(rowInline);

        rowInline = new ArrayList<>();
        rowInline.add(InlineKeyboardButton.builder().text("Авторизация студента").callbackData("login_student").build());
        rowInline.add(InlineKeyboardButton.builder().text("Авторизация преподавателя").callbackData("login_teacher").build());
        rowsInline.add(rowInline);

        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        sendMsg(message);
    }

    private void sendUsernameMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Введите ваш логин:");

        sendMsg(message);
    }

    private void sendPasswordMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Введите ваш пароль:");

        sendMsg(message);
    }

    private void processRegistrationOrLogin(long chatId, UserState userState) {
        if ("student".equals(userState.role) || "teacher".equals(userState.role)) {
            if (userState.state == State.ENTER_PASSWORD) {
                registerOrLoginUser(chatId, userState);
            }
        } else {
            sendUnknownCommandMessage(chatId);
        }
    }

    private void registerOrLoginUser(long chatId, UserState userState) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            User user = session.get(User.class, userState.username);
            if (user == null) {
                // Registration
                user = new User();
                user.setUsername(userState.username);
                user.setPassword(userState.password);
                user.setRole(userState.role);
                session.save(user);
                sendMsg(chatId, "Регистрация успешна!");
                logger.info("User {} with role {} successfully registered", userState.username, userState.role);
            } else {
                // Login
                if (user.getPassword().equals(userState.password) && user.getRole().equals(userState.role)) {
                    sendMsg(chatId, "Авторизация успешна!");
                    logger.info("User {} with role {} successfully logged in", userState.username, userState.role);
                } else {
                    sendMsg(chatId, "Неправильный логин, пароль или роль.");
                    logger.warn("Failed login attempt for user {} with role {}", userState.username, userState.role);
                }
            }
            transaction.commit();
        } catch (Exception e) {
            sendMsg(chatId, "Ошибка при регистрации/авторизации.");
            logger.error("Error during registration/login for user {}", userState.username, e);
        } finally {
            userStates.remove(chatId);
        }
    }

    private void sendUnknownCommandMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Неизвестная команда. Используйте /start для начала.");

        sendMsg(message);
    }

    private void sendMsg(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Error sending message", e);
        }
    }

    private void sendMsg(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        sendMsg(message);
    }

    private void deleteMessage(Message message) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(message.getChatId().toString());
        deleteMessage.setMessageId(message.getMessageId());

        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            logger.error("Error deleting message", e);
        }
    }
}