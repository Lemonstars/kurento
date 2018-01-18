package cn.superid.controller;

import cn.superid.bean.form.ChatContentForm;
import cn.superid.entity.Room;
import cn.superid.entity.User;
import cn.superid.manager.RoomManagerInterface;
import cn.superid.manager.UserManagerInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
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
    private UserManagerInterface userManager;

    @Autowired
    private RoomManagerInterface roomManager;

    @MessageMapping("/chatSend")
    public void sendChat(ChatContentForm chatContentForm){
        String roomId = chatContentForm.getRoomId();
        simpMessagingTemplate.convertAndSend("/topic/chatContent-" + roomId,chatContentForm);
    }

    @MessageMapping("/applyForHost/{userId}")
    public void applyForHost(@DestinationVariable String userId){
        User applyUser = userManager.getByUserId(userId);
        String roomId = applyUser.getRoomId();
        Room currentRoom = roomManager.getRoom(roomId);
        User presenter = currentRoom.getPresenter();
        if(presenter != null){
            simpMessagingTemplate.convertAndSend("/queue/receiveApply", userId);
        }
    }

}
