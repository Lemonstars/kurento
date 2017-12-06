package cn.superid.room;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.kurento.client.KurentoClient;
import org.springframework.beans.factory.annotation.Autowired;

public class RoomManager {

    @Autowired
    private KurentoClient kurento;

    private final ConcurrentMap<String, Room> rooms = new ConcurrentHashMap<>();

    public Room getRoom(String roomName) {
        Room room = rooms.get(roomName);

        if (room == null) {
            room = new Room(roomName, kurento.createMediaPipeline());
            rooms.put(roomName, room);
        }
        return room;
    }

    public void removeRoom(Room room) {
        rooms.remove(room.getRoomName());
        room.close();
    }

}
