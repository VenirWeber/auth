package ru.telegram_auth.Commands;

import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.MaybeInaccessibleMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.telegram_auth.models.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import ru.telegram_auth.JPA.UserRepository;
import ru.telegram_auth.JPA.UserRepositoryImpl;

import java.util.*;

public class NGB extends TelegramLongPollingBot {
    private static final Logger logger = LoggerFactory.getLogger(NGB.class);
    private final Map<Long, UserState> userStates = new HashMap<>();
    private final UserRepository userRepository = new UserRepositoryImpl();
    private final Map<Long, Integer> lastBotMessages = new HashMap<>();

    private enum State {
        CHOOSE_ROLE, ENTER_USERNAME, ENTER_PASSWORD
    }

    private static class UserState {
        State state;
        String role;
        String username;
        String password;
    }

    public NGB() {
        super();
    }

    @Override
    public String getBotUsername() {
        return "@never_gonna_bot";
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
            handleCallbackQuery(update.getCallbackQuery());
        }
    }

    private void handleTextMessage(Message message) {
        long chatId = message.getChatId();
        String text = message.getText();
        int messageId = message.getMessageId();

        if ("/start".equals(text)) {
            sendRoleSelectionMessage(chatId);
        } else {
            UserState userState = userStates.get(chatId);
            if (userState != null) {
                deleteLastBotMessage(chatId);  // Delete last bot message before processing the new one
                deleteMessage(chatId, messageId);  // Delete the user's message
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

    private void handleCallbackQuery(CallbackQuery callbackQuery) {
        MaybeInaccessibleMessage message = callbackQuery.getMessage();
        if (message == null) {
            logger.error("Message is null in handleCallbackQuery");
            return;
        }

        long chatId = message.getChatId();
        int messageId = message.getMessageId();
        UserState userState = userStates.getOrDefault(chatId, new UserState());

        deleteLastBotMessage(chatId);  // Delete the previous bot message

        switch (callbackQuery.getData()) {
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
        deleteMessage(chatId, messageId);  // Delete the user's callback query message
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

        sendMsgAndTrack(message, chatId);
    }

    private void sendUsernameMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Введите ваш логин:");

        sendMsgAndTrack(message, chatId);
    }

    private void sendPasswordMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Введите ваш пароль:");

        sendMsgAndTrack(message, chatId);
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
        User user = userRepository.findByUsername(userState.username);

        if (user == null) {
            // Registration
            user = new User();
            user.setTelegramId(chatId);
            user.setUsername(userState.username);
            user.setPassword(userState.password);
            user.setRole(userState.role);
            userRepository.save(user);
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

        userStates.remove(chatId);
    }

    private void sendUnknownCommandMessage(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText("Неизвестная команда. Используйте /start для начала.");

        sendMsgAndTrack(message, chatId);
    }

    private void sendMsgAndTrack(SendMessage message, long chatId) {
        try {
            Message sentMessage = execute(message);
            lastBotMessages.put(chatId, sentMessage.getMessageId());
        } catch (TelegramApiException e) {
            logger.error("Error sending message", e);
        }
    }

    private void sendMsg(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        sendMsgAndTrack(message, chatId);
    }

    private void deleteMessage(long chatId, int messageId) {
        DeleteMessage deleteMessage = new DeleteMessage();
        deleteMessage.setChatId(Long.toString(chatId));
        deleteMessage.setMessageId(messageId);

        try {
            execute(deleteMessage);
        } catch (TelegramApiException e) {
            if (e.getMessage().contains("message to delete not found")) {
                logger.warn("Message to delete not found: {}", messageId);
            } else {
                logger.error("Error deleting message", e);
            }
        }
    }

    private void deleteLastBotMessage(long chatId) {
        Integer lastBotMessageId = lastBotMessages.remove(chatId);
        if (lastBotMessageId != null) {
            deleteMessage(chatId, lastBotMessageId);
        }
    }
}
