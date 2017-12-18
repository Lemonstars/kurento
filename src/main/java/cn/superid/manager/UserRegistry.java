package cn.superid.manager;

import cn.superid.entity.User;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;

public class UserRegistry {

  private final ConcurrentHashMap<String, User> usersByName = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, User> usersBySessionId = new ConcurrentHashMap<>();

  public void register(User user) {
    usersByName.put(user.getName(), user);
    usersBySessionId.put(user.getSession().getId(), user);
  }

  public User getByName(String name) {
    return usersByName.get(name);
  }

  public User getBySession(WebSocketSession session) {
    return usersBySessionId.get(session.getId());
  }

  public boolean exists(String name) {
    return usersByName.keySet().contains(name);
  }

  public User removeBySession(WebSocketSession session) {
    final User user = getBySession(session);
    usersByName.remove(user.getName());
    usersBySessionId.remove(session.getId());
    return user;
  }

}
