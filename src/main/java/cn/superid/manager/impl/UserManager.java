package cn.superid.manager.impl;

import java.util.concurrent.ConcurrentHashMap;

import cn.superid.entity.User;
import cn.superid.manager.UserManagerInterface;
import org.springframework.web.socket.WebSocketSession;

/**
 * 用户管理
 *
 * @author 刘兴
 * @date 2017-12-6
 * @version 1.0
 */
public class UserManager implements UserManagerInterface{

    private final ConcurrentHashMap<String, User> usersByUserId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, User> usersBySessionId = new ConcurrentHashMap<>();

    @Override
    public void register(User user) {
        usersByUserId.put(user.getUserId(), user);
        usersBySessionId.put(user.getSession().getId(), user);
    }

    @Override
    public User removeBySession(WebSocketSession session) {
        User user = getBySession(session);
        if(user!=null){
            usersByUserId.remove(user.getUserId());
            usersBySessionId.remove(session.getId());
        }
        return user;
    }

    @Override
    public User getByUserId(String userId) {
        return usersByUserId.get(userId);
    }

    @Override
    public User getBySession(WebSocketSession session) {
        return usersBySessionId.get(session.getId());
    }

    @Override
    public boolean isUserFree(String userName) {
        return !usersByUserId.containsKey(userName);
    }
}
