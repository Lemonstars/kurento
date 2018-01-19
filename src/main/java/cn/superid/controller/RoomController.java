package cn.superid.controller;

import cn.superid.bean.form.RoomJoinForm;
import cn.superid.bean.vo.ResponseVO;
import cn.superid.constant.ErrorCode;
import cn.superid.entity.Room;
import cn.superid.entity.User;
import cn.superid.service.RoomService;
import cn.superid.service.UserService;
import cn.superid.util.ResponseUtil;
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
    private UserService userService;

    @Autowired
    private RoomService roomService;

    @MessageMapping("/createRoom/{userId}")
    public void createRoom(@DestinationVariable String userId){
        ResponseVO responseVO;
        if(userService.isUserFree(userId)){
            Room newRoom = roomService.create();
            responseVO = ResponseUtil.successResponse(newRoom.getRoomId());
        }else {
            responseVO = ResponseUtil.errorResponse(ErrorCode.USER_ON_VIDEO);
        }
        simpMessagingTemplate.convertAndSend("/queue/roomId-" + userId, responseVO);
    }

    @MessageMapping("/joinRoom")
    public void joinRoom(RoomJoinForm roomJoinForm){
        String userId = roomJoinForm.getUserId();
        String roomId = roomJoinForm.getRoomId();
        String sdpOffer = roomJoinForm.getSdpOffer();
        boolean isPresenter = roomJoinForm.getIsPresenter();

        ResponseVO responseVO;
        if(!userService.isUserFree(userId)){
            responseVO = ResponseUtil.errorResponse(ErrorCode.USER_ON_VIDEO);
        }else if(!roomService.isRoomExist(roomId)){
            responseVO = ResponseUtil.errorResponse(ErrorCode.ROOM_NOT_EXIST);
        }else {
            Room room = roomService.getRoom(roomId);

            User user = new User(userId, roomId, isPresenter, room.getPipeline());
            userService.register(user);
            user.processSdpOffer(sdpOffer, simpMessagingTemplate);

            room.joinRoom(user);

            responseVO= ResponseUtil.successResponse(userId);
        }

        simpMessagingTemplate.convertAndSend("/topic/joinUserId-" + roomId, responseVO);
    }

    @MessageMapping("/leaveRoom/{userId}")
    public void leaveRoom(@DestinationVariable String userId){
        if(!userService.isUserFree(userId)){
            User userToQuit = userService.getByUserId(userId);
            userService.removeByUserId(userId);

            String roomId = userToQuit.getRoomId();
            roomService.leaveRoom(userId, roomId);

            simpMessagingTemplate.convertAndSend("/topic/leftUserId-" + roomId, ResponseUtil.successResponse(userId));
        }

    }

}
