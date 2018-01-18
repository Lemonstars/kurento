package cn.superid.controller;

import cn.superid.bean.form.ChatContentForm;
import cn.superid.bean.vo.ChatContentVO;
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

    @MessageMapping("/chatSend")
    public void sendChatContent(ChatContentForm form){
        String roomId = form.getRoomId();
        ChatContentVO vo = new ChatContentVO(form.getUserId(), form.getChatContent());
        simpMessagingTemplate.convertAndSend("/topic/chatContent-" + roomId, ResponseUtil.successResponse(vo));
    }


}
