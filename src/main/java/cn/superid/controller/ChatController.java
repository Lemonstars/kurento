package cn.superid.controller;

import cn.superid.bean.form.ApplyAcceptForm;
import cn.superid.bean.form.ChatContentForm;
import cn.superid.entity.Room;
import cn.superid.entity.User;
import cn.superid.service.RoomService;
import cn.superid.service.UserService;
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
    private UserService userService;

    @Autowired
    private RoomService roomService;

    @MessageMapping("/chatSend")
    public void sendChat(ChatContentForm chatContentForm){
        String roomId = chatContentForm.getRoomId();
        simpMessagingTemplate.convertAndSend("/topic/chatContent-" + roomId,chatContentForm);
    }

    @MessageMapping("/applyForHost/{userId}")
    public void applyForHost(@DestinationVariable String userId){
        User applyUser = userService.getByUserId(userId);
        String roomId = applyUser.getRoomId();
        Room currentRoom = roomService.getRoom(roomId);
        User presenter = currentRoom.getPresenter();
        if(presenter != null){
            simpMessagingTemplate.convertAndSend("/queue/receiveApply-" + presenter.getUserId(), userId);
        }
    }

    @MessageMapping("/refuseApply/{applyUserId}")
    public void refuseApply(@DestinationVariable String applyUserId){
        simpMessagingTemplate.convertAndSend("/queue/applyRefused-" + applyUserId, applyUserId);
    }

    @MessageMapping("/acceptApply")
    public void acceptApply(ApplyAcceptForm applyAcceptForm){
        String presenterId = applyAcceptForm.getPresenterId();
        String applierId = applyAcceptForm.getApplierId();

        User currentPresenter = userService.getByUserId(presenterId);
        User applyUser = userService.getByUserId(applierId);

        String roomId = applyUser.getRoomId();
        Room currentRoom = roomService.getRoom(roomId);
        currentRoom.changeCameraHost(currentPresenter, applyUser);
    }

}
