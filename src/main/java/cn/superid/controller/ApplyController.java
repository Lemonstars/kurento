package cn.superid.controller;

import cn.superid.bean.form.ApplyAcceptForm;
import cn.superid.service.ApplyService;
import cn.superid.service.UserService;
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
public class ApplyController {

    @Autowired
    private ApplyService applyService;

    @Autowired
    private UserService userService;

    @MessageMapping("/applyForHost/{userId}")
    public void handleApply(@DestinationVariable String userId){
        if(!userService.isUserFree(userId)){
            applyService.applyForHost(userId);
        }
    }

    @MessageMapping("/refuseApply/{applyUserId}")
    public void refuseApply(@DestinationVariable String applyUserId){
        if(!userService.isUserFree(applyUserId)){
            applyService.refuseApply(applyUserId);
        }
    }

    @MessageMapping("/acceptApply")
    public void acceptApply(ApplyAcceptForm form){
        String presenterId = form.getPresenterId();
        String applierId = form.getApplierId();
        if(!userService.isUserFree(presenterId) &&
                !userService.isUserFree(applierId)){
            applyService.acceptApply(presenterId, applierId);
        }
    }

}
