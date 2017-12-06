package cn.superid.user;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.web.socket.WebSocketSession;

public class UserRegistry {

    private final ConcurrentHashMap<String, UserSession> usersByName = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, UserSession> usersBySessionId = new ConcurrentHashMap<>();

    public void register(UserSession user) {
        usersByName.put(user.getUserName(), user);
        usersBySessionId.put(user.getSession().getId(), user);
    }

    public UserSession getByName(String name) {
        return usersByName.get(name);
    }

    public UserSession getBySession(WebSocketSession session) {
        return usersBySessionId.get(session.getId());
    }

    public UserSession removeBySession(WebSocketSession session) {
        UserSession user = getBySession(session);
        usersByName.remove(user.getUserName());
        usersBySessionId.remove(session.getId());
        return user;
    }

}
