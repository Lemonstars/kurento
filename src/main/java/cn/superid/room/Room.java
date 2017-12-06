package cn.superid.room;

import cn.superid.user.UserSession;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.kurento.client.MediaPipeline;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Room implements Closeable {

    private String roomName;
    private MediaPipeline pipeline;
    private ConcurrentMap<String, UserSession> participants = new ConcurrentHashMap<>();

    public Room(String roomName, MediaPipeline pipeline) {
        this.roomName = roomName;
        this.pipeline = pipeline;
    }

    @Override
    public void close() {
        for (UserSession user : participants.values()) {
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

    public String getRoomName() {
        return roomName;
    }

    public UserSession join(String userName, WebSocketSession session) throws IOException {
        UserSession participant = new UserSession(userName, this.roomName, session, this.pipeline);
        joinRoom(participant);
        participants.put(participant.getUserName(), participant);
        sendParticipantNames(participant);
        return participant;
    }

    public void leave(UserSession user) throws IOException {
        this.removeParticipant(user.getUserName());
        user.close();
    }

    private void joinRoom(UserSession newParticipant) throws IOException {
        JsonObject newParticipantMsg = new JsonObject();
        newParticipantMsg.addProperty("id", "newParticipantArrived");
        newParticipantMsg.addProperty("name", newParticipant.getUserName());

        for (final UserSession participant : participants.values()) {
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
        for (final UserSession participant : participants.values()) {
            try {
                participant.cancelVideoFrom(name);
                participant.sendMessage(participantLeftJson);
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }


    }

    public void sendParticipantNames(UserSession user) throws IOException {

        JsonArray participantsArray = new JsonArray();
        for (UserSession participant : this.getParticipants()) {
            if (!participant.equals(user)) {
                JsonElement participantName = new JsonPrimitive(participant.getUserName());
                participantsArray.add(participantName);
            }
        }

        JsonObject existingParticipantsMsg = new JsonObject();
        existingParticipantsMsg.addProperty("id", "existingParticipants");
        existingParticipantsMsg.add("data", participantsArray);
        user.sendMessage(existingParticipantsMsg);
    }

    public Collection<UserSession> getParticipants() {
        return participants.values();
    }

}
