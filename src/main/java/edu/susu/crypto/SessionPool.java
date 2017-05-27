package edu.susu.crypto;

import edu.susu.database.User;

import java.util.HashMap;
import java.util.Map;

/**
 * Массив сеансов пользователей
 */
public class SessionPool {
    Map<String, Session> sessions = new HashMap<String, Session>();

    /**
     * Открывает новую сессию для пользователя
     * @param user представление пользователя
     * @param timeToLiveMinutes срок сеанса, мин
     * @return ключ новой сессии
     */
    public String openSession(User user, long timeToLiveMinutes) {
        Session session = new Session(user, timeToLiveMinutes);
        String key = generateSessionKey(session);
        sessions.put(key, session);
        return key;
    }

    public void closeSession(String sessionKey) {
        Session session = sessions.get(sessionKey);
        if (session != null)
            session.close();
        sessions.remove(sessionKey);
    }

    public void closeAllSessions() {
        for (String sessionKey : sessions.keySet())
            closeSession(sessionKey);
    }

    /**
     * Возвращает сессию по ключу
     * @param key ключ
     * @return объект Session, null если по такому ключу нет сессий
     */
    public Session getSession(String key) {
        return sessions.get(key);
    }

    /**
     * Закрывает все сеансы с истекшим сроком
     */
    public void cleanExpired() {
        for (String key : sessions.keySet())
            if (sessions.get(key).isExpired())
                sessions.remove(key);
    }

    private String generateSessionKey(Session session) {
        // вероятно, один и тот же юзер не сможет залогиниться два раза в один момент времени
        int userHash = session.getUser().hashCode();
        int timeHash = session.getCreationTime().hashCode();
        return Integer.toHexString(userHash) + Integer.toHexString(timeHash);
    }
}
