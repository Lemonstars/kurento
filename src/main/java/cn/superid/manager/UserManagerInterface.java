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
     * 通过姓名获取用户
     * @param name
     * @return
     */
    User getByName(String name);

    /**
     * 通过session获取用户
     * @param session
     * @return
     */
    User getBySession(WebSocketSession session);

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
}
