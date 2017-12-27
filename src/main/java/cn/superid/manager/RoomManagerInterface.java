package cn.superid.manager;

import cn.superid.entity.Room;

/**
 * @author 刘兴
 * @version 1.0
 * @date 2017/12/18
 */
public interface RoomManagerInterface {

    /**
     * 根据房间名获取房间：
     * 如果房间不存在，则创建一个；若存在，则返回
     *
     * @param roomName
     * @return
     */
    Room getRoom(String roomName);

    /**
     * 通过房间标识判断房间是否存在
     * @param roomId
     * @return true 房间存在
     */
    boolean isRoomExist(String roomId);

    /**
     * 移除房间记录
     * @param room
     */
    void removeRoom(Room room);

}
