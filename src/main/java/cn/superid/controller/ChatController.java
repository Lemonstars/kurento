package cn.superid.controller;

import cn.superid.bean.form.ChatContentForm;
import cn.superid.bean.vo.ChatContentVO;
import cn.superid.entity.User;
import cn.superid.service.RoomService;
import cn.superid.service.UserService;
import cn.superid.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

/**
 * @author 刘兴
 * @version 1.0
 * @date 2018/01/18
 */

@Controller
public class ChatController {

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private RoomService roomService;

    @MessageMapping("/chatSend")
    public void sendChatContent(ChatContentForm form){
        String userId = form.getUserId();
        String chatContent = form.getChatContent();

        if(!userService.isUserFree(userId)){
            User chatUser = userService.getByUserId(userId);
            String roomId = chatUser.getRoomId();

            ChatContentVO vo = new ChatContentVO(userId, chatContent);
            simpMessagingTemplate.convertAndSend("/topic/chatContent-" + roomId, ResponseUtil.successResponse(vo));
        }

    }


}
