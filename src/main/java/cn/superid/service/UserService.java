package cn.superid.service;

import cn.superid.entity.User;

/**
 * @author 刘兴
 * @version 1.0
 * @date 2018/01/11
 */
public interface UserService {

    /**
     * 通过用户标识获取用户
     * @param userId
     * @return
     */
    User getByUserId(String userId);

    /**
     * 通过用户标识清除用户数据
     * @param userId
     * @return
     */
    void removeByUserId(String userId);

    /**
     * 存储用户记录
     * @param user
     */
    void register(User user);

    /**
     * 判断用户是否在视频通话中:
     * false 在通话中
     *
     * @param userId
     * @return
     */
    boolean isUserFree(String userId);

}