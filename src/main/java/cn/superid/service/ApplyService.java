package cn.superid.service;

/**
 * @author 刘兴
 * @version 1.0
 * @date 2018/01/18
 */
public interface ApplyService {

    /**
     * 申请使用摄像头
     * @param userId
     */
    void applyForHost(String userId);

    /**
     * 拒绝申请
     * @param applyUserId
     */
    void refuseApply(String applyUserId);

    /**
     * 接受申请
     * @param presenterId 当前使用摄像头的用户标识
     * @param applierId 申请使用摄像头的用户标识
     */
    void acceptApply(String presenterId, String applierId);
}
