package cn.superid.service.impl;

import cn.superid.entity.Room;
import cn.superid.entity.User;
import cn.superid.service.ApplyService;
import cn.superid.service.RoomService;
import cn.superid.service.UserService;
import cn.superid.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * @author 刘兴
 * @version 1.0
 * @date 2018/01/18
 */
@Service
public class ApplyServiceImpl implements ApplyService{

    @Autowired
    private SimpMessagingTemplate simpMessagingTemplate;

    @Autowired
    private UserService userService;

    @Autowired
    private RoomService roomService;

    @Override
    public void applyForHost(String applyUserId) {
        User applyUser = userService.getByUserId(applyUserId);
        String roomId = applyUser.getRoomId();
        Room currentRoom = roomService.getRoom(roomId);
        User presenter = currentRoom.getPresenter();
        if(presenter != null){
            simpMessagingTemplate.convertAndSend("/queue/receiveApply-" + presenter.getUserId(), ResponseUtil.successResponse(applyUserId));
        }
    }

    @Override
    public void refuseApply(String applyUserId) {
        simpMessagingTemplate.convertAndSend("/queue/applyRefused-" + applyUserId, ResponseUtil.successResponse(applyUserId));
    }

    @Override
    public void acceptApply(String presenterId, String applierId) {
        User currentPresenter = userService.getByUserId(presenterId);
        User applyUser = userService.getByUserId(applierId);

        String roomId = applyUser.getRoomId();
        Room currentRoom = roomService.getRoom(roomId);
        currentRoom.changeCameraHost(currentPresenter, applyUser);
    }
}
