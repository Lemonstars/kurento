package cn.superid.manager;

import cn.superid.entity.User;

/**
 * @author 刘兴
 * @version 1.0
 * @date 2017/12/18
 */
public interface UserManagerInterface {

    /**
     * 通过用户标识获取用户
     * @param userId
     * @return
     */
    User getByUserId(String userId);

    /**
     * 通过session获取用户
     * @param sessionId
     * @return
     */
    User getBySessionId(String sessionId);

    /**
     * 通过用户标识清除用户数据
     * @param userId
     * @return
     */
    User removeByUserId(String userId);

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
