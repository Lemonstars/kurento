package cn.superid.handler;

import cn.superid.entity.Room;
import cn.superid.entity.User;
import cn.superid.manager.RoomManagerInterface;
import cn.superid.manager.UserManagerInterface;
import cn.superid.util.UUIDGenerator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.kurento.client.IceCandidate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

public class GroupCallHandler extends TextWebSocketHandler {

    private static final Gson gson = new GsonBuilder().create();

    @Autowired
    private RoomManagerInterface roomManager;

    @Autowired
    private UserManagerInterface userManager;

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);

        User user = userManager.getBySession(session);

        switch (jsonMessage.get("id").getAsString()) {
            case "createRoom":
                createRoom(jsonMessage, session);
                break;
            case "receiveVideoFrom":
                String senderName = jsonMessage.get("sender").getAsString();
                User sender = userManager.getByName(senderName);
                String sdpOffer = jsonMessage.get("sdpOffer").getAsString();
                user.receiveVideoFrom(sender, sdpOffer);
                break;
            case "leaveRoom":
                leaveRoom(user);
                break;
            case "onIceCandidate":
                JsonObject candidate = jsonMessage.get("candidate").getAsJsonObject();

                if (user != null) {
                    IceCandidate cand = new IceCandidate(candidate.get("candidate").getAsString(),
                            candidate.get("sdpMid").getAsString(), candidate.get("sdpMLineIndex").getAsInt());
                    user.addCandidate(cand, jsonMessage.get("name").getAsString());
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        User user = userManager.removeBySession(session);
        roomManager.getRoom(user.getRoomId()).leave(user);
    }
//
//    private void createRoom(JsonObject params, WebSocketSession session) throws IOException {
//        String name = params.get("name").getAsString();
//        String roomId = UUIDGenerator.generatorUUID();
//
//        Room room = roomManager.getRoom(roomId);
//        User user = room.join(name, session);
//        userManager.register(user);
//    }

    private void createRoom(JsonObject params, WebSocketSession session) throws IOException {
        String name = params.get("name").getAsString();
        String roomId = UUIDGenerator.generatorUUID();

        boolean isFree = userManager.isUserFree(name);
        if(isFree){
            Room room = roomManager.getRoom(roomId);
            User user = room.join(name, session);
            userManager.register(user);
        }else {
            User user = userManager.getByName(name);
            user.notifyVideoExist();
        }
    }

    private void leaveRoom(User user) throws IOException {
        Room room = roomManager.getRoom(user.getRoomId());
        room.leave(user);
        if (!room.existParticipants()) {
            roomManager.removeRoom(room);
        }
    }
}
