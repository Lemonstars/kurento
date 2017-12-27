package cn.superid.manager.impl;

import cn.superid.entity.Room;
import cn.superid.manager.RoomManagerInterface;
import org.kurento.client.Composite;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author 刘兴
 * @date 2017-12-18
 */
public class RoomManagerImpl implements RoomManagerInterface{

    private final Logger log = LoggerFactory.getLogger(RoomManagerImpl.class);

    @Autowired
    private KurentoClient kurento;

    private ConcurrentMap<String, Room> rooms = new ConcurrentHashMap<>();

    @Override
    public Room getRoom(String roomId) {
        log.info("Searching for room {}", roomId);
        Room room = rooms.get(roomId);

        if (room == null) {
            log.info("Room {} not exist. Will create now!", roomId);

            MediaPipeline pipeline = kurento.createMediaPipeline();

            room = new Room(roomId, pipeline);
            rooms.put(roomId, room);
        }
        log.info("Room {} found!", roomId);
        return room;
    }

    @Override
    public boolean isRoomExist(String roomId) {
        return rooms.containsKey(roomId);
    }

    @Override
    public void removeRoom(Room room) {
        rooms.remove(room.getRoomId());
        room.close();
        log.info("Room {} removed and closed", room.getRoomId());
    }

}
