package cn.superid.manager;

import cn.superid.entity.User;
import org.springframework.web.socket.WebSocketSession;

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
     * 通过sessionI获取用户
     * @param sessionId
     * @return
     */
    User getBySessionId(String sessionId);

    /**
     * 通过session清除用户记录
     * @param session
     * @return
     */
    User removeBySession(WebSocketSession session);

    /**
     * 存储用户记录
     * @param user
     */
    void register(User user);

    /**
     * 判断用户是否在视频通话中:
     * true 在通话中
     *
     * @param userId
     * @return
     */
    boolean isUserFree(String userId);
}
