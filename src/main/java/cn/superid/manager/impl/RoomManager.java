package cn.superid.manager.impl;

import cn.superid.entity.Room;
import cn.superid.manager.RoomManagerInterface;
import org.kurento.client.KurentoClient;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 房间管理
 *
 * @author 刘兴
 * @date 2017.12.6
 * @version 1.0
 */
public class RoomManager implements RoomManagerInterface{

    @Autowired
    private KurentoClient kurento;

    private final ConcurrentMap<String, Room> rooms = new ConcurrentHashMap<>();

    @Override
    public Room getRoom(String roomId) {
        Room room = rooms.get(roomId);

        if (!rooms.containsKey(roomId)) {
            room = new Room(roomId, kurento.createMediaPipeline());
            rooms.put(roomId, room);
        }
        return room;
    }

    @Override
    public void removeRoom(Room room) {
        rooms.remove(room.getRoomId());
        room.close();
    }

}
