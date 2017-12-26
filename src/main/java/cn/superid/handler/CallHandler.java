package cn.superid.handler;

import cn.superid.entity.Room;
import cn.superid.entity.User;
import cn.superid.manager.RoomManagerInterface;
import cn.superid.manager.UserManagerInterface;
import cn.superid.util.UUIDGenerator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private UserManagerInterface userManager;

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        JsonObject jsonMessage = gson.fromJson(message.getPayload(), JsonObject.class);

        switch (jsonMessage.get("id").getAsString()) {
            case "createRoom":
                createRoom(jsonMessage, session);
                break;
            case "joinRoom":
                joinRoom(jsonMessage, session);
                break;
            case "startVideo":
                startVideo(jsonMessage, session);
                break;
            case "onIceCandidate": {
                JsonObject jsonCandidate = jsonMessage.get("candidate").getAsJsonObject();

                User user = userManager.getBySessionId(session.getId());
                if (user != null) {
                    IceCandidate candidate = new IceCandidate(jsonCandidate.get("candidate").getAsString(),
                            jsonCandidate.get("sdpMid").getAsString(),
                            jsonCandidate.get("sdpMLineIndex").getAsInt());
                    user.addCandidate(candidate);
                }
                break;
            }
            default:
                break;
        }
    }


    private void createRoom(JsonObject params, WebSocketSession session) throws IOException {
        String userId = params.get("userId").getAsString();

        if(userManager.isUserFree(userId)){
            String roomId = UUIDGenerator.generatorUUID();

            log.info("PARTICIPANT {}: trying to join room {}", userId, roomId);

            Room room = roomManager.getRoom(roomId);
            User presenter = new User(userId, roomId, true, room.getPipeline(), session);
            userManager.register(presenter);
            presenter.notifyPresenterRoomId(roomId);

        }else {
            log.info("User {} is on another video", userId);

            User user = userManager.getByUserId(userId);
            user.notifyUserBusy();
        }

    }

    private void  startVideo(JsonObject params, WebSocketSession session){
        String userId = params.get("userId").getAsString();
        User user = userManager.getByUserId(userId);
        WebRtcEndpoint webRtcEndpoint = user.getWebRtcEndpoint();
        webRtcEndpoint.connect(webRtcEndpoint);

        String sdpOffer = params.get("sdpOffer").getAsString();
        String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

        JsonObject response = new JsonObject();
        response.addProperty("id", "startResponse");
        response.addProperty("sdpAnswer", sdpAnswer);

        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(response.toString()));
            }
        }catch (IOException e){
            e.printStackTrace();
        }

        webRtcEndpoint.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {
            @Override
            public void onEvent(IceCandidateFoundEvent event) {
                JsonObject response = new JsonObject();
                response.addProperty("id", "iceCandidate");
                response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
                try {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(response.toString()));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        webRtcEndpoint.gatherCandidates();

    }

    private void joinRoom(JsonObject params, WebSocketSession session) throws IOException{
        String userId = params.get("userId").getAsString();

        if(userManager.isUserFree(userId)){
            String roomId = params.get("roomId").getAsString();
            log.info("PARTICIPANT {}: trying to join room {}", userId, roomId);

            Room room = roomManager.getRoom(roomId);
            User viewer = new User(userId, roomId, false, room.getPipeline(), session);
            userManager.register(viewer);
        }else {
            log.info("User {} is on another video", userId);

            User user = userManager.getByUserId(userId);
            user.notifyUserBusy();
        }

    }

}
