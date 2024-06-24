package ru.telegram_auth.JPA;

import ru.telegram_auth.models.User;

import java.util.List;

public interface UserRepository {
    void save(User user);
    User findByUsername(String username);
    User findByTelegramId(Long telegramId);
    List<User> findAll();
}