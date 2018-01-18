package cn.superid.service;

import cn.superid.entity.Room;

/**
 * @author 刘兴
 * @version 1.0
 * @date 2018/01/11
 */
public interface RoomService {


    /**
     * 通过房间标识判断房间是否存在
     * @param roomId
     * @return true 房间存在
     */
    boolean isRoomExist(String roomId);

    /**
     * 移除房间记录
     * @param roomId
     */
    void removeRoom(String roomId);

    /**
     * 创建房间
     * @param userId
     * @return
     */
    Room create(String userId);


    /**
     * 根据房间名获取房间
     * @param roomId
     * @return
     */
    Room getRoom(String roomId);

}