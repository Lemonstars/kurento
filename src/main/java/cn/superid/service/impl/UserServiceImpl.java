package cn.superid.service.impl;

import cn.superid.entity.User;
import cn.superid.service.UserService;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author 刘兴
 * @version 1.0
 * @date 2018/01/11
 */
@Service
public class UserServiceImpl implements UserService {

    private ConcurrentHashMap<String, User> usersByUserId = new ConcurrentHashMap<>();

    @Override
    public void register(User user) {
        usersByUserId.put(user.getUserId(), user);
    }

    @Override
    public User getByUserId(String userId) {
        return usersByUserId.get(userId);
    }

    @Override
    public User removeByUserId(String userId) {
        User user = getByUserId(userId);
        usersByUserId.remove(userId);
        return user;
    }

    @Override
    public boolean isUserFree(String userId) {
        return !usersByUserId.containsKey(userId);
    }


}