package cn.superid.manager.impl;

import cn.superid.entity.User;
import cn.superid.manager.UserManagerInterface;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 刘兴
 * @date 2017-12-18
 */
public class UserManagerImpl implements UserManagerInterface{

    private ConcurrentHashMap<String, User> usersByUserId = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, User> usersBySessionId = new ConcurrentHashMap<>();

    @Override
    public void register(User user) {
        usersByUserId.put(user.getUserId(), user);
        usersBySessionId.put(user.getSession().getId(), user);
    }

    @Override
    public User getByUserId(String userId) {
        return usersByUserId.get(userId);
    }

    @Override
    public User getBySessionId(String sessionId) {
        return usersBySessionId.get(sessionId);
    }

    @Override
    public User removeByUserId(String userId) {
        User user = getByUserId(userId);
        usersByUserId.remove(userId);
        usersBySessionId.remove(user.getSession().getId());
        return user;
    }

    @Override
    public boolean isUserFree(String userId) {
        return !usersByUserId.containsKey(userId);
    }
}
