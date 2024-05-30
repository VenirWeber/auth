package org.telegram;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.junit.Before;
import org.junit.Test;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.telegram_auth.HibernateUtil;
import ru.telegram_auth.NGB;
import ru.telegram_auth.User;

import static org.mockito.Mockito.*;

public class MyTelegramBotTest {
    private NGB bot;
    private Session session;
    private Transaction transaction;

    @Before
    public void setUp() {
        bot = new NGB();
        SessionFactory sessionFactory = mock(SessionFactory.class);
        session = mock(Session.class);
        transaction = mock(Transaction.class);

        when(sessionFactory.openSession()).thenReturn(session);
        when(session.beginTransaction()).thenReturn(transaction);
        HibernateUtil.setSessionFactory(sessionFactory);
    }

    @Test
    public void testHandleLogin_Success() {
        User user = new User();
        user.setUsername("student");
        user.setPassword("password");
        user.setRole("student");

        when(session.get(User.class, "student")).thenReturn(user);

        Update update = createUpdate("/login student student password");
        bot.onUpdateReceived(update);

        verify(session).get(User.class, "student");
        verify(session).close();
    }

    @Test
    public void testHandleLogin_Failure() {
        when(session.get(User.class, "student")).thenReturn(null);

        Update update = createUpdate("/login student student wrongpassword");
        bot.onUpdateReceived(update);

        verify(session).get(User.class, "student");
        verify(session).close();
    }

    @Test
    public void testHandleRegister_Success() {
        Update update = createUpdate("/register_student student password");
        bot.onUpdateReceived(update);

        verify(session).save(any(User.class));
        verify(transaction).commit();
        verify(session).close();
    }

    @Test
    public void testHandleRegister_Failure() {
        doThrow(new RuntimeException()).when(session).save(any(User.class));

        Update update = createUpdate("/register_student student password");
        bot.onUpdateReceived(update);

        verify(session).save(any(User.class));
        verify(session).close();
    }

    private Update createUpdate(String text) {
        Message message = mock(Message.class);
        when(message.getText()).thenReturn(text);
        when(message.getChatId()).thenReturn(123456789L);

        Update update = mock(Update.class);
        when(update.getMessage()).thenReturn(message);

        return update;
    }
}