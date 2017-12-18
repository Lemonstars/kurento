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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

/**
 * @author 刘兴
 * @date 2017-12-18
 */
public class CallHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(CallHandler.class);

    private static final Gson gson = new GsonBuilder().create();

    @Autowired
    private RoomManagerInterface roomManager;

    @Autowired
    private UserManagerInterface registry;

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);

        User user = registry.getBySession(session);

        if (user != null) {
            log.debug("Incoming message from user '{}': {}", user.getUserId(), jsonMessage);
        } else {
            log.debug("Incoming message from new user: {}", jsonMessage);
        }

        switch (jsonMessage.get("id").getAsString()) {
            case "createRoom":
                createRoom(jsonMessage, session);
                break;
            case "joinRoom":
                joinRoom(jsonMessage, session);
                break;
            case "receiveVideoFrom":
                String senderName = jsonMessage.get("sender").getAsString();
                User sender = registry.getByName(senderName);
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
        User user = registry.removeBySession(session);
        roomManager.getRoom(user.getRoomId()).leave(user);
    }

    private void createRoom(JsonObject params, WebSocketSession session) throws IOException {
        String roomId = UUIDGenerator.generatorUUID();
        log.info("ROOMID {}", roomId);

        String userId = params.get("userId").getAsString();
        log.info("PARTICIPANT {}: trying to join room {}", userId, roomId);

        Room room = roomManager.getRoom(roomId);
        User user = room.join(userId, session);
        registry.register(user);
    }

    private void joinRoom(JsonObject params, WebSocketSession session) throws IOException{
        String roomId = params.get("roomId").getAsString();
        String userId = params.get("userId").getAsString();
        log.info("PARTICIPANT {}: trying to join room {}", userId, roomId);

        Room room = roomManager.getRoom(roomId);
        User user = room.join(userId, session);
        registry.register(user);
    }

    private void leaveRoom(User user) throws IOException {
        Room room = roomManager.getRoom(user.getRoomId());
        room.leave(user);
        if (room.getParticipants().isEmpty()) {
            roomManager.removeRoom(room);
        }
    }

}
