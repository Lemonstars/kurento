package cn.superid.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.kurento.client.MediaPipeline;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 视频房间
 *
 * @author 刘兴
 * @date 2017.12.6
 * @version 1.0
 */
public class Room implements Closeable {

    private String roomId;
    private MediaPipeline pipeline;
    private ConcurrentMap<String, User> participants = new ConcurrentHashMap<>();

    public Room(String roomId, MediaPipeline pipeline) {
        this.roomId = roomId;
        this.pipeline = pipeline;
    }

    /**
     * 创建房间创建者加入房间：
     * 1.自己加入房间
     * 2.将房间标识传递给client
     *
     * @param userId
     * @param session
     * @return
     * @throws IOException
     */
    public User joinNewRoom(String userId, WebSocketSession session) throws IOException{
        User roomCreator = new User(userId, this.roomId, session, this.pipeline);
        notifyClientRoomId(roomCreator);
        //TODO 当前信息传递和前端显示情况下，需要调用这个方法
        joinRoom(roomCreator);
        participants.put(userId, roomCreator);
        sendParticipantNames(roomCreator);

        return roomCreator;
    }

    /**
     * 新成员加入房间：
     * 1.通知已在房间内的成员：有新成员加入，获取用户名
     * 2.通知刚加入的成员：所有其他已在房间内成员的用户名
     *
     * @param userId
     * @param session
     * @return 生成的用户对象
     * @throws IOException
     */
    public User joinExistingRoom(String userId, WebSocketSession session) throws IOException {
        User participant = new User(userId, this.roomId, session, this.pipeline);
        joinRoom(participant);
        participants.put(userId, participant);
        sendParticipantNames(participant);
        return participant;
    }

    /**
     * 成员退出房间:
     * 通知其他成员：有成员退出房间
     *
     * @param user
     * @throws IOException
     */
    public void leave(User user) throws IOException {
        this.removeParticipant(user.getUserId());
        user.close();
    }

    /**
     * 房间内是否还存在成员
     * @return true 房间内有成员
     */
    public boolean existParticipants(){
        return !participants.values().isEmpty();
    }

    /**
     * 获取房间的标志
     * @return
     */
    public String getRoomId() {
        return roomId;
    }

    private void joinRoom(User newParticipant) throws IOException {
        JsonObject newParticipantMsg = new JsonObject();
        newParticipantMsg.addProperty("id", "newParticipantArrived");
        newParticipantMsg.addProperty("name", newParticipant.getUserId());

        for (User participant : participants.values()) {
            try {
                participant.sendMessage(newParticipantMsg);
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void removeParticipant(String name) throws IOException {
        participants.remove(name);

        JsonObject participantLeftJson = new JsonObject();
        participantLeftJson.addProperty("id", "participantLeft");
        participantLeftJson.addProperty("name", name);

        for (final User participant : participants.values()) {
            try {
                participant.cancelVideoFrom(name);
                participant.sendMessage(participantLeftJson);
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendParticipantNames(User user) throws IOException {
        JsonArray participantsArray = new JsonArray();
        for (User participant : participants.values()) {
            if (!participant.equals(user)) {
                JsonElement participantName = new JsonPrimitive(participant.getUserId());
                participantsArray.add(participantName);
            }
        }

        JsonObject existingParticipantsMsg = new JsonObject();
        existingParticipantsMsg.addProperty("id", "existingParticipants");
        existingParticipantsMsg.add("data", participantsArray);
        user.sendMessage(existingParticipantsMsg);
    }

    private void notifyClientRoomId(User user) throws IOException{
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "captureRoomId");
        jsonObject.addProperty("roomId", user.getRoomId());

        user.sendMessage(jsonObject);
    }

    @Override
    public void close() {
        for (User user : participants.values()) {
            try {
                user.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        participants.clear();
        pipeline.release( );
    }

    @PreDestroy
    private void shutdown() {
        this.close();
    }

}
