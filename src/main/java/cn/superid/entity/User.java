package cn.superid.entity;

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
 * 视频会议用户
 *
 * @author 刘兴
 * @date 2017-12-6
 * @version 1.0
 */
public class User implements Closeable {

    private String userId;
    private String roomId;
    private WebSocketSession session;
    private MediaPipeline pipeline;

    private WebRtcEndpoint outgoingMedia;
    private ConcurrentMap<String, WebRtcEndpoint> incomingMedia = new ConcurrentHashMap<>();

    public User(String userId, String roomId, final WebSocketSession session, MediaPipeline pipeline) {
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
                    e.printStackTrace();
                }
            }
        });
    }

    public String getUserId() {
        return userId;
    }

    public String getRoomId() {
        return roomId;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public WebRtcEndpoint getOutgoingMedia() {
        return outgoingMedia;
    }

    public void sendMessage(JsonObject message) throws IOException {
        synchronized (session) {
            session.sendMessage(new TextMessage(message.toString()));
        }
    }

    /**
     * 通知client用户已在其他的视频聊天中
     */
    public void notifyVideoExist(String userId) throws IOException{
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "videoExist");
        jsonObject.addProperty("userId", userId);
        sendMessage(jsonObject);
    }

    public void cancelVideoFrom(String senderName) {
        WebRtcEndpoint incoming = incomingMedia.remove(senderName);
        incoming.release();
    }

    public void  receiveVideoFrom(User sender, String sdpOffer) throws IOException{
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
                        e.printStackTrace();
                    }
                }
            });

            incomingMedia.put(sender.getUserId(), incoming);
        }

        sender.getOutgoingMedia().connect(incoming);

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
    public void close() throws IOException {
        for (String remoteParticipantName : incomingMedia.keySet()) {
            WebRtcEndpoint ep = this.incomingMedia.get(remoteParticipantName);
            ep.release();
        }
        outgoingMedia.release();
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
}
