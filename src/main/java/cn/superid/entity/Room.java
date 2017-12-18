package cn.superid.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.kurento.client.Continuation;
import org.kurento.client.MediaPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author 刘兴
 * @date 2017-12-18
 */
public class Room implements Closeable {

    private final Logger log = LoggerFactory.getLogger(Room.class);

    private String roomId;
    private MediaPipeline pipeline;
    private ConcurrentMap<String, User> participants = new ConcurrentHashMap<>();

    public String getRoomId() {
        return roomId;
    }

    public Room(String roomId, MediaPipeline pipeline) {
        this.roomId = roomId;
        this.pipeline = pipeline;
        log.info("ROOM {} has been created", roomId);
    }

    @PreDestroy
    private void shutdown() {
        this.close();
    }

    public User join(String userName, WebSocketSession session) throws IOException {
        log.info("ROOM {}: adding participant {}", userName, userName);
        User participant = new User(userName, this.roomId, session, this.pipeline);
        joinRoom(participant);
        participants.put(participant.getUserId(), participant);
        sendParticipantNames(participant);
        return participant;
    }

    public void leave(User user) throws IOException {
        log.debug("PARTICIPANT {}: Leaving room {}", user.getUserId(), this.roomId);
        this.removeParticipant(user.getUserId());
        user.close();
    }

    private Collection<String> joinRoom(User newParticipant) throws IOException {
        JsonObject newParticipantMsg = new JsonObject();
        newParticipantMsg.addProperty("id", "newParticipantArrived");
        newParticipantMsg.addProperty("name", newParticipant.getUserId());

        List<String> participantsList = new ArrayList<>(participants.values().size());
        log.debug("ROOM {}: notifying other participants of new participant {}", roomId,
                newParticipant.getUserId());

        for (User participant : participants.values()) {
            try {
                participant.sendMessage(newParticipantMsg);
            } catch (IOException e) {
                log.debug("ROOM {}: participant {} could not be notified", roomId, participant.getUserId(), e);
            }
            participantsList.add(participant.getUserId());
        }

        return participantsList;
    }

    private void removeParticipant(String name) throws IOException {
        participants.remove(name);

        log.debug("ROOM {}: notifying all users that {} is leaving the room", this.roomId, name);

        List<String> unnotifiedParticipants = new ArrayList<>();
        JsonObject participantLeftJson = new JsonObject();
        participantLeftJson.addProperty("id", "participantLeft");
        participantLeftJson.addProperty("name", name);
        for (User participant : participants.values()) {
            try {
                participant.cancelVideoFrom(name);
                participant.sendMessage(participantLeftJson);
            } catch (IOException e) {
                unnotifiedParticipants.add(participant.getUserId());
            }
        }

        if (!unnotifiedParticipants.isEmpty()) {
            log.debug("ROOM {}: The users {} could not be notified that {} left the room", this.roomId,
                    unnotifiedParticipants, name);
        }

    }

    public void sendParticipantNames(User user) throws IOException {
        JsonArray participantsArray = new JsonArray();
        for (User participant : this.getParticipants()) {
            if (!participant.equals(user)) {
                JsonElement participantName = new JsonPrimitive(participant.getUserId());
                participantsArray.add(participantName);
            }
        }

        JsonObject existingParticipantsMsg = new JsonObject();
        existingParticipantsMsg.addProperty("id", "existingParticipants");
        existingParticipantsMsg.add("data", participantsArray);
        log.debug("PARTICIPANT {}: sending a list of {} participants", user.getUserId(), participantsArray.size());
        user.sendMessage(existingParticipantsMsg);
    }

    public Collection<User> getParticipants() {
        return participants.values();
    }

    public User getParticipant(String name) {
        return participants.get(name);
    }

    @Override
    public void close() {
        for (User user : participants.values()) {
            try {
                user.close();
            } catch (IOException e) {
                log.debug("ROOM {}: Could not invoke close on participant {}", this.roomId, user.getUserId(),
                        e);
            }
        }

        participants.clear();

        pipeline.release(new Continuation<Void>() {

            @Override
            public void onSuccess(Void result) throws Exception {
                log.trace("ROOM {}: Released Pipeline", Room.this.roomId);
            }

            @Override
            public void onError(Throwable cause) throws Exception {
                log.warn("PARTICIPANT {}: Could not release Pipeline", Room.this.roomId);
            }
        });

        log.debug("Room {} closed", this.roomId);
    }

}
