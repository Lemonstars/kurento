package cn.superid.manager;

import cn.superid.entity.Room;

/**
 * @author 刘兴
 * @version 1.0
 * @date 2017/12/06
 */
public interface RoomManagerInterface {

    /**
     * 通过房间名获取房间
     * 如果该房间名对应的房间不存在，则创建一个新房间
     * @param roomName
     * @return
     */
    Room getRoom(String roomName);

    /**
     * 关闭房间
     * @param roomInterface
     */
    void removeRoom(Room roomInterface);

}
