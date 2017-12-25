package cn.superid.entity;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author 刘兴
 * @date 2017-12-18
 */
public class User implements Closeable {
    private String userId;
    private String roomId;
    private WebSocketSession session;
    private MediaPipeline pipeline;

    private WebRtcEndpoint outgoingMedia;
    private ConcurrentMap<String, WebRtcEndpoint> incomingMedia = new ConcurrentHashMap<>();

    public User(String userId, String roomId, final WebSocketSession session,
                MediaPipeline pipeline) {
        this.userId = userId;
        this.roomId = roomId;
        this.session = session;
        this.pipeline = pipeline;

        this.outgoingMedia = new WebRtcEndpoint.Builder(pipeline).build();

        this.outgoingMedia.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

            @Override
            public void onEvent(IceCandidateFoundEvent event) {
                JsonObject response = new JsonObject();
                response.addProperty("id", "iceCandidate");
                response.addProperty("name", userId);
                response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
                try {
                    synchronized (session) {
                        session.sendMessage(new TextMessage(response.toString()));
                    }
                } catch (IOException e) {
                }
            }
        });
    }

    public String getUserId() {
        return userId;
    }

    public String getRoomId() {
        return this.roomId;
    }

    public WebSocketSession getSession() {
        return session;
    }

    @Override
    public void close() throws IOException {
        for (final String remoteParticipantName : incomingMedia.keySet()) {
            final WebRtcEndpoint ep = this.incomingMedia.get(remoteParticipantName);
            ep.release();
        }
        outgoingMedia.release();
    }

    private void sendMessage(JsonObject message) throws IOException {
        synchronized (session) {
            session.sendMessage(new TextMessage(message.toString()));
        }
    }

    void cancelVideoFrom(final String senderName) {
        final WebRtcEndpoint incoming = incomingMedia.remove(senderName);
        incoming.release();
    }


    public void receiveVideoFrom(User sender, String sdpOffer) throws IOException{
        WebRtcEndpoint webRtcEndpoint = getEndpointForUser(sender);
        String ipSdpAnswer = webRtcEndpoint.processOffer(sdpOffer);

        JsonObject scParams = new JsonObject();
        scParams.addProperty("id", "receiveVideoAnswer");
        scParams.addProperty("name", sender.getUserId());
        scParams.addProperty("sdpAnswer", ipSdpAnswer);

        sendMessage(scParams);
        getEndpointForUser(sender).gatherCandidates();
    }


    private WebRtcEndpoint getEndpointForUser(User sender) {
        if (sender.getUserId().equals(userId)) {
            return outgoingMedia;
        }


        WebRtcEndpoint incoming = incomingMedia.get(sender.getUserId());
        if (incoming == null) {
            incoming = new WebRtcEndpoint.Builder(pipeline).build();

            incoming.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {
                @Override
                public void onEvent(IceCandidateFoundEvent event) {
                    JsonObject response = new JsonObject();
                    response.addProperty("id", "iceCandidate");
                    response.addProperty("name", sender.getUserId());
                    response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
                    try {
                        synchronized (session) {
                            session.sendMessage(new TextMessage(response.toString()));
                        }
                    } catch (IOException e) {
                    }
                }
            });

            incomingMedia.put(sender.getUserId(), incoming);
        }

        sender.outgoingMedia.connect(incoming);

        return incoming;
    }

    public void addCandidate(IceCandidate candidate, String name) {
        if (this.userId.compareTo(name) == 0) {
            outgoingMedia.addIceCandidate(candidate);
        } else {
            WebRtcEndpoint webRtc = incomingMedia.get(name);
            if (webRtc != null) {
                webRtc.addIceCandidate(candidate);
            }
        }
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null || !(obj instanceof User)) {
            return false;
        }
        User other = (User) obj;
        boolean eq = userId.equals(other.userId);
        eq &= roomId.equals(other.roomId);
        return eq;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + userId.hashCode();
        result = 31 * result + roomId.hashCode();
        return result;
    }


    /**
     * 通知用户已加入其它的视频会议
     * @throws IOException
     */
    public void notifyUserBusy() throws IOException{
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "userState");

        sendMessage(jsonObject);
    }

    /**
     * 通知视频发起者房间的标识
     * @param roomId
     * @throws IOException
     */
    public void notifyPresenterRoomId(String roomId) throws IOException{
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "roomId");
        jsonObject.addProperty("roomId", roomId);

        sendMessage(jsonObject);
    }

    /**
     * 通知有新用户加入，告知用户标识
     * @param userId
     * @throws IOException
     */
    void notifyNewUserId(String userId) throws IOException{
        JsonObject newParticipantMsg = new JsonObject();
        newParticipantMsg.addProperty("id", "newParticipantArrived");
        newParticipantMsg.addProperty("name", userId);

        sendMessage(newParticipantMsg);
    }

    /**
     * 在刚加入房间时，通知在房间内其他用户的标识
     * @param existingUsers
     * @throws IOException
     */
    void notifyExistingUserId(JsonArray existingUsers) throws IOException{
        JsonObject existingParticipantsMsg = new JsonObject();
        existingParticipantsMsg.addProperty("id", "existingParticipants");
        existingParticipantsMsg.add("data", existingUsers);
        sendMessage(existingParticipantsMsg);
    }

    /**
     * 其他用户离开时，通知房间内成员离开成员的用户标识
     * @param userId
     * @throws IOException
     */
    void notifyUserLeft(String userId) throws IOException{
        JsonObject participantLeftJson = new JsonObject();
        participantLeftJson.addProperty("id", "participantLeft");
        participantLeftJson.addProperty("name", userId);

        sendMessage(participantLeftJson);
    }

}
