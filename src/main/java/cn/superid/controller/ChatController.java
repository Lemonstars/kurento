package cn.superid.controller;

import cn.superid.bean.form.ChatContentForm;
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

    @MessageMapping("/chatSend")
    public void sendChat(ChatContentForm chatContentForm){
        String roomId = chatContentForm.getRoomId();
        simpMessagingTemplate.convertAndSend("/topic/chatContent-" + roomId,chatContentForm);
    }

}
