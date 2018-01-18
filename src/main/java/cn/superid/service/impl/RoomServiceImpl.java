package cn.superid.service.impl;

import cn.superid.entity.Room;
import cn.superid.service.RoomService;
import cn.superid.util.UUIDGeneratorUtil;
import org.kurento.client.KurentoClient;
import org.kurento.client.MediaPipeline;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author 刘兴
 * @version 1.0
 * @date 2018/01/11
 */
@Service
public class RoomServiceImpl implements RoomService {

    @Autowired
    private KurentoClient kurento;

    private ConcurrentMap<String, Room> rooms = new ConcurrentHashMap<>();

    @Override
    public boolean isRoomExist(String roomId) {
        return rooms.containsKey(roomId);
    }

    @Override
    public void removeRoom(String roomId) {
        rooms.remove(roomId);
    }

    @Override
    public Room create(String userId) {
        //create a new room and register it
        String roomId = UUIDGeneratorUtil.generatorUUID();
        MediaPipeline pipeline = kurento.createMediaPipeline();
        Room createRoom = new Room(roomId, pipeline);
        rooms.put(roomId, createRoom);

        return createRoom;
    }

    @Override
    public Room getRoom(String roomId) {
        return rooms.get(roomId);
    }

}