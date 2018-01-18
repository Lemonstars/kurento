package cn.superid.controller;

import cn.superid.entity.User;
import cn.superid.manager.UserManagerInterface;
import org.kurento.client.IceCandidate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * @author 刘兴
 * @version 1.0
 * @date 2018/01/18
 */
@Controller
public class IceCandidateController {

    @Autowired
    private UserManagerInterface userManager;

    @MessageMapping("/onIceCandidate/{userId}")
    public void onIceCandidate(@DestinationVariable String userId, IceCandidate iceCandidate){
        User user = userManager.getByUserId(userId);
        if(user != null){
            user.addCandidate(iceCandidate);
        }
    }

}
