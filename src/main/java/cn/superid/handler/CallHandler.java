package cn.superid.handler;

import cn.superid.entity.Room;
import cn.superid.entity.User;
import cn.superid.manager.RoomManagerInterface;
import cn.superid.manager.UserManagerInterface;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.kurento.client.IceCandidate;
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
            case "joinVideo":
                joinVideo(jsonMessage, session);
                break;
            case "leaveRoom":
                leaveRoom(jsonMessage);
                break;
            case "chatSend":
                chatSend(jsonMessage);
                break;
            case "applyForHost":
                applyForHost(jsonMessage);
                break;
            case "refuseApply":
                refuseApply(jsonMessage);
                break;
            case "acceptApply":
                acceptApply(jsonMessage);
                break;
            default:
                break;
        }
    }

    private void joinVideo(JsonObject params, WebSocketSession session) throws IOException{
        String userId = params.get("userId").getAsString();
        String roomId = params.get("roomId").getAsString();

        if(!roomManager.isRoomExist(roomId)){
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", "roomState");
            jsonObject.addProperty("state", "room not exist");

            session.sendMessage(new TextMessage(jsonObject.toString()));
            return;
        }

        if(!userManager.isUserFree(userId)){
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("id", "userState");
            jsonObject.addProperty("state", "user is an anther video");

            session.sendMessage(new TextMessage(jsonObject.toString()));
            return;
        }

        Room room = roomManager.getRoom(roomId);
        User viewer = new User(userId, roomId, false, room.getPipeline());
        userManager.register(viewer);

        String sdpOffer = params.get("sdpOffer").getAsString();
//        room.joinRoom(viewer, sdpOffer);
    }

    private void leaveRoom(JsonObject params) throws IOException{
        String userId = params.get("userId").getAsString();

        User userToQuit = userManager.getByUserId(userId);
        userToQuit.close();
        userManager.removeByUserId(userId);

        String roomId = userToQuit.getRoomId();
        Room room = roomManager.getRoom(roomId);
        room.removeUserId(userId);

        if(room.isRoomEmpty()){
            room.close();
            roomManager.removeRoom(roomId);
        }else {
            room.notifySomeoneLeft(userId);
        }
    }

    private void chatSend(JsonObject params) throws IOException{
        String roomId = params.get("roomId").getAsString();
        String userId = params.get("userId").getAsString();
        String content = params.get("content").getAsString();

        Room room = roomManager.getRoom(roomId);
        room.transferChatContent(content, userId);
    }

    private void applyForHost(JsonObject params) throws IOException{
        String userId = params.get("userId").getAsString();
        User applyUser = userManager.getByUserId(userId);
        String roomId = applyUser.getRoomId();
        Room currentRoom = roomManager.getRoom(roomId);
        User presenter = currentRoom.getPresenter();
        if(presenter != null){
            presenter.notifyApplyForHost(userId);
        }
    }

    private void refuseApply(JsonObject params) throws IOException{
        String applyUserId = params.get("applyUserId").getAsString();
        User user = userManager.getByUserId(applyUserId);
        user.notifyApplyRefused();
    }

    private void acceptApply(JsonObject params) throws IOException{
        String userId = params.get("userId").getAsString();
        User currentPresenter = userManager.getByUserId(userId);

        String applyUserId = params.get("applyUserId").getAsString();
        User applyUser = userManager.getByUserId(applyUserId);

        String roomId = applyUser.getRoomId();
        Room currentRoom = roomManager.getRoom(roomId);
        currentRoom.changeCameraHost(currentPresenter, applyUser);
    }
}

