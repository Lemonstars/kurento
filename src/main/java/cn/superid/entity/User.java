package cn.superid.entity;

import com.google.gson.JsonObject;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.Closeable;
import java.io.IOException;

/**
 * @author 刘兴
 * @date 2017-12-18
 */
public class User implements Closeable {

    private String userId;
    private String roomId;
    private boolean isPresenter;
    private MediaPipeline mediaPipeline;
    private WebRtcEndpoint webRtcEndpoint;
    private final WebSocketSession session;

    public User(String userId, String roomId, boolean isPresenter, MediaPipeline mediaPipeline, WebSocketSession session) {
        this.userId = userId;
        this.roomId = roomId;
        this.isPresenter = isPresenter;
        this.mediaPipeline = mediaPipeline;
        this.session = session;

        this.webRtcEndpoint = new WebRtcEndpoint.Builder(mediaPipeline).build();
//        this.webRtcEndpoint.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {
//            @Override
//            public void onEvent(IceCandidateFoundEvent event) {
//                JsonObject response = new JsonObject();
//                response.addProperty("id", "iceCandidate");
//                response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
//                try {
//                    synchronized (session) {
//                        session.sendMessage(new TextMessage(response.toString()));
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        });
//        webRtcEndpoint.gatherCandidates();
    }

    public WebRtcEndpoint getWebRtcEndpoint() {
        return webRtcEndpoint;
    }

    public String getUserId() {
        return userId;
    }

    public WebSocketSession getSession() {
        return session;
    }

    @Override
    public void close() throws IOException {

    }

    private void sendMessage(JsonObject message) throws IOException {
        synchronized (session) {
            session.sendMessage(new TextMessage(message.toString()));
        }
    }

    public void addCandidate(IceCandidate candidate) {
        webRtcEndpoint.addIceCandidate(candidate);
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

}
