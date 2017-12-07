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
     * 新成员加入房间：
     * 1.通知已在房间内的成员：有新成员加入，获取用户名
     * 2.通知刚加入的成员：所有其他已在房间内成员的用户名
     *
     * @param userName
     * @param session
     * @return 生成的用户对象
     * @throws IOException
     */
    public User join(String userName, WebSocketSession session) throws IOException {
        User participant = new User(userName, this.roomId, session, this.pipeline);
        joinRoom(participant);
        participants.put(participant.getUserId(), participant);
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
