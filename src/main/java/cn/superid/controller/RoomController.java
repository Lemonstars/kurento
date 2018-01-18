package cn.superid.controller;

import cn.superid.bean.form.RoomJoinForm;
import cn.superid.entity.Room;
import cn.superid.entity.User;
import cn.superid.manager.RoomManagerInterface;
import cn.superid.manager.UserManagerInterface;
import cn.superid.util.ResponseUtil;
import cn.superid.util.UUIDGenerator;
import com.google.gson.JsonObject;
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
public class RoomController {

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private UserManagerInterface userManager;

    @Autowired
    private RoomManagerInterface roomManager;

    @MessageMapping("/createRoom/{userId}")
    public void createRoom(@DestinationVariable String userId){
        String roomId = "";
        if(userManager.isUserFree(userId)){
            roomId = UUIDGenerator.generatorUUID();

            Room room = roomManager.getRoom(roomId);
            User presenter = new User(userId, roomId, true, room.getPipeline());
            userManager.register(presenter);
            presenter.notifyPresenterRoomId(roomId);

        }else {

            User user = userManager.getByUserId(userId);
            user.notifyUserBusy();
        }

        simpMessagingTemplate.convertAndSend("/queue/roomId-" + userId, ResponseUtil.successResponse(roomId));
    }

    @MessageMapping("/startVideo/{userId}")
    public void startVideo(@DestinationVariable String userId, String sdpOffer){
        User presenter = userManager.getByUserId(userId);
        String roomId = presenter.getRoomId();
        Room room = roomManager.getRoom(roomId);

        room.joinRoom(presenter, sdpOffer, simpMessagingTemplate);
    }

    @MessageMapping("/joinVideo")
    public void joinVideo(RoomJoinForm roomJoinForm){
        String userId = roomJoinForm.getUserId();
        String roomId = roomJoinForm.getRoomId();
        String sdpOffer = roomJoinForm.getSdpOffer();

        if(!roomManager.isRoomExist(roomId)){
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", "roomState");
            jsonObject.addProperty("state", "room not exist");

            return;
        }

        if(!userManager.isUserFree(userId)){
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", "userState");
            jsonObject.addProperty("state", "user is an anther video");

            return;
        }

        Room room = roomManager.getRoom(roomId);
        User viewer = new User(userId, roomId, false, room.getPipeline());
        userManager.register(viewer);

        room.joinRoom(viewer, sdpOffer, simpMessagingTemplate);
    }

}
