package cn.superid.user;

import com.google.gson.JsonObject;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class UserSession implements Closeable {

    private final String userName;
    private final String roomName;
    private final WebSocketSession session;
    private final MediaPipeline pipeline;

    private final WebRtcEndpoint outgoingMedia;
    private final ConcurrentMap<String, WebRtcEndpoint> incomingMedia = new ConcurrentHashMap<>();

    public UserSession(String userName, String roomName, final WebSocketSession session, MediaPipeline pipeline) {
        this.userName = userName;
        this.roomName = roomName;
        this.session = session;
        this.pipeline = pipeline;

        this.outgoingMedia = new WebRtcEndpoint.Builder(pipeline).build();

        this.outgoingMedia.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

            @Override
            public void onEvent(IceCandidateFoundEvent event) {
                JsonObject response = new JsonObject();
                response.addProperty("id", "iceCandidate");
                response.addProperty("name", userName);
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

    public String getUserName() {
        return userName;
    }

    public String getRoomName() {
        return this.roomName;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public WebRtcEndpoint getOutgoingWebRtcPeer() {
        return outgoingMedia;
    }


    @Override
    public void close() throws IOException {
        for (final String remoteParticipantName : incomingMedia.keySet()) {
            final WebRtcEndpoint ep = this.incomingMedia.get(remoteParticipantName);
            ep.release();
        }
        outgoingMedia.release();
    }

    public void sendMessage(JsonObject message) throws IOException {
        synchronized (session) {
            session.sendMessage(new TextMessage(message.toString()));
        }
    }

    public void cancelVideoFrom(final String senderName) {
        final WebRtcEndpoint incoming = incomingMedia.remove(senderName);
        incoming.release();
    }


    public void receiveVideoFrom(UserSession sender, String sdpOffer) throws IOException{
        WebRtcEndpoint webRtcEndpoint = getEndpointForUser(sender);
        String ipSdpAnswer = webRtcEndpoint.processOffer(sdpOffer);


        JsonObject scParams = new JsonObject();
        scParams.addProperty("id", "receiveVideoAnswer");
        scParams.addProperty("name", sender.getUserName());
        scParams.addProperty("sdpAnswer", ipSdpAnswer);

        sendMessage(scParams);
        getEndpointForUser(sender).gatherCandidates();
    }


    private WebRtcEndpoint getEndpointForUser(final UserSession sender) {
        if (sender.getUserName().equals(userName)) {
            return outgoingMedia;
        }


        WebRtcEndpoint incoming = incomingMedia.get(sender.getUserName());
        if (incoming == null) {
            incoming = new WebRtcEndpoint.Builder(pipeline).build();

            incoming.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

                @Override
                public void onEvent(IceCandidateFoundEvent event) {
                    JsonObject response = new JsonObject();
                    response.addProperty("id", "iceCandidate");
                    response.addProperty("name", sender.getUserName());
                    response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
                    try {
                        synchronized (session) {
                            session.sendMessage(new TextMessage(response.toString()));
                        }
                    } catch (IOException e) {
                    }
                }
            });

            incomingMedia.put(sender.getUserName(), incoming);
        }

        sender.getOutgoingWebRtcPeer().connect(incoming);

        return incoming;
    }

    public void addCandidate(IceCandidate candidate, String name) {
        if (this.userName.compareTo(name) == 0) {
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
        if (obj == null || !(obj instanceof UserSession)) {
            return false;
        }

        UserSession other = (UserSession) obj;
        boolean eq = userName.equals(other.userName);
        eq &= roomName.equals(other.roomName);
        return eq;
    }

    @Override
    public int hashCode() {
        int result = 1;
        result = 31 * result + userName.hashCode();
        result = 31 * result + roomName.hashCode();
        return result;
    }
}
