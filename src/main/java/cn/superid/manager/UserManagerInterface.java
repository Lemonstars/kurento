package cn.superid.manager;

import cn.superid.entity.User;
import org.springframework.web.socket.WebSocketSession;

/**
 * @author 刘兴
 * @version 1.0
 * @date 2017/12/06
 */
public interface UserManagerInterface {

    /**
     * 注册用户
     * @param user
     */
    void register(User user);

    /**
     * 通过session清除用户数据
     * @param session
     * @return
     */
    User removeBySession(WebSocketSession session);

    /**
     * 通过用户名清除用户数据
     * @param userName
     * @return
     */
    User removeByUserName(String userName);

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
     * 通过用户名判断用户是否空闲
     * @param userName
     * @return false 如果在通话中
     */
    boolean isUserFree(String userName);
}
