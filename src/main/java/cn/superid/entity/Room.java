package cn.superid.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.kurento.client.Composite;
import org.kurento.client.Continuation;
import org.kurento.client.MediaPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
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
    private Composite composite;
    private MediaPipeline pipeline;
    private ConcurrentMap<String, User> participants = new ConcurrentHashMap<>();

    public Room(String roomId, MediaPipeline pipeline, Composite composite) {
        this.roomId = roomId;
        this.pipeline = pipeline;
        this.composite = composite;
        log.info("ROOM {} has been created", roomId);
    }

    @PreDestroy
    private void shutdown() {
        this.close();
    }

    public User join(String userName, boolean isPresenter, WebSocketSession session) throws IOException {
        log.info("ROOM {}: adding participant {}", roomId, userName);
        User participant = new User(userName, this.roomId, isPresenter, session, this.pipeline);
        joinRoom(participant);
        participants.put(participant.getUserId(), participant);
        sendParticipantNames(participant);
        return participant;
    }

    public void leave(User user) throws IOException {
        log.info("PARTICIPANT {}: Leaving room {}", user.getUserId(), this.roomId);
        this.removeParticipant(user.getUserId());
        user.close();
    }

    private void joinRoom(User newParticipant) throws IOException {
        log.info("ROOM {}: notifying other participants of new participant {}", roomId, newParticipant.getUserId());

        String userId = newParticipant.getUserId();
        for (User participant : participants.values()) {
            try {
                participant.notifyNewUserId(userId);
            } catch (IOException e) {
               log.info("ROOM {}: participant {} could not be notified", roomId, participant.getUserId(), e);
            }
        }
    }

    private void removeParticipant(String userId) throws IOException {
        participants.remove(userId);

        log.info("ROOM {}: notifying all users that {} is leaving the room", this.roomId, userId);

        List<String> unnotifiedParticipants = new ArrayList<>();
        for (User participant : participants.values()) {
            try {
                participant.cancelVideoFrom(userId);
                participant.notifyUserLeft(userId);
            } catch (IOException e) {
                unnotifiedParticipants.add(participant.getUserId());
            }
        }

        if (!unnotifiedParticipants.isEmpty()) {
           log.info("ROOM {}: The users {} could not be notified that {} left the room", this.roomId, unnotifiedParticipants, userId);
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

        log.info("PARTICIPANT {}: sending a list of {} participants", user.getUserId(), participantsArray.size());
        user.notifyExistingUserId(participantsArray);
    }

    public boolean isRoomEmpty(){
        return participants.values().isEmpty();
    }

    public String getRoomId() {
        return roomId;
    }

    @Override
    public void close() {
        for (User user : participants.values()) {
            try {
                user.close();
            } catch (IOException e) {
               log.info("ROOM {}: Could not invoke close on participant {}", this.roomId, user.getUserId(), e);
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

       log.info("Room {} closed", this.roomId);
    }

}
