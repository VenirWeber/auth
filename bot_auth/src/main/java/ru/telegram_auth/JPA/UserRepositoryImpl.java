package ru.telegram_auth.JPA;

import org.hibernate.Session;
import org.hibernate.Transaction;
import ru.telegram_auth.models.User;
import ru.telegram_auth.JPA.HibernateUtil;

import java.util.List;

public class UserRepositoryImpl implements UserRepository {
    @Override
    public void save(User user) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            Transaction transaction = session.beginTransaction();
            session.saveOrUpdate(user);
            transaction.commit();
        }
    }

    @Override
    public User findByUsername(String username) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM User WHERE username = :username", User.class)
                    .setParameter("username", username)
                    .uniqueResult();
        }
    }

    @Override
    public User findByTelegramId(Long telegramId) {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM User WHERE telegramId = :telegramId", User.class)
                    .setParameter("telegramId", telegramId)
                    .uniqueResult();
        }
    }

    @Override
    public List<User> findAll() {
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            return session.createQuery("FROM User", User.class).list();
        }
    }
}

