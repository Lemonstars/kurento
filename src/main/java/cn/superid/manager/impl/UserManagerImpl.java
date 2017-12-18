package cn.superid.manager.impl;

import cn.superid.entity.User;
import cn.superid.manager.UserManagerInterface;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 刘兴
 * @date 2017-12-18
 */
public class UserManagerImpl implements UserManagerInterface{

  private final ConcurrentHashMap<String, User> usersByName = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, User> usersBySessionId = new ConcurrentHashMap<>();

  @Override
  public void register(User user) {
    usersByName.put(user.getUserId(), user);
    usersBySessionId.put(user.getSession().getId(), user);
  }

  @Override
  public User getByName(String name) {
    return usersByName.get(name);
  }

  @Override
  public User getBySession(WebSocketSession session) {
    return usersBySessionId.get(session.getId());
  }

  @Override
  public User removeBySession(WebSocketSession session) {
    final User user = getBySession(session);
    usersByName.remove(user.getUserId());
    usersBySessionId.remove(session.getId());
    return user;
  }

}
