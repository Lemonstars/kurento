package cn.superid.entity;

import com.google.gson.JsonObject;
import org.kurento.client.HubPort;
import org.kurento.client.IceCandidate;
import org.kurento.client.MediaPipeline;
import org.kurento.client.WebRtcEndpoint;
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
    private WebRtcEndpoint webRtcEndpoint;
    private HubPort hubPort;
    private final WebSocketSession session;

    public User(String userId, String roomId, boolean isPresenter, MediaPipeline mediaPipeline, WebSocketSession session) {
        this.userId = userId;
        this.roomId = roomId;
        this.isPresenter = isPresenter;
        this.session = session;

        this.webRtcEndpoint = new WebRtcEndpoint.Builder(mediaPipeline).build();
    }

    public WebRtcEndpoint getWebRtcEndpoint() {
        return webRtcEndpoint;
    }

    public String getUserId() {
        return userId;
    }

    public String getRoomId() {
        return roomId;
    }

    public boolean isPresenter() {
        return isPresenter;
    }

    public void setPresenter(boolean presenter) {
        isPresenter = presenter;
    }

    public HubPort getHubPort() {
        return hubPort;
    }

    public void setHubPort(HubPort hubPort) {
        this.hubPort = hubPort;
    }

    private void sendMessage(JsonObject message){
        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(message.toString()));
            }
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    public void addCandidate(IceCandidate candidate) {
        webRtcEndpoint.addIceCandidate(candidate);
    }

    @Override
    public void close() throws IOException {
        webRtcEndpoint.release();
    }

    /**
     * 通知用户已加入其它的视频会议
     */
    public void notifyUserBusy(){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "userState");

        sendMessage(jsonObject);
    }

    /**
     * 通知视频发起者房间的标识
     * @param roomId
     */
    public void notifyPresenterRoomId(String roomId){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "roomId");
        jsonObject.addProperty("roomId", roomId);

        sendMessage(jsonObject);
    }

    /**
     * 通知聊天内容
     * @param content
     */
    public void notifyChatContent(String content, String senderId){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "chatContent");
        jsonObject.addProperty("senderId", senderId);
        jsonObject.addProperty("content", content);

        sendMessage(jsonObject);
    }

    /**
     * 将离开房间的用户的标识通知给其他人
     * @param leftUserId
     */
    public void notifyLeftUserId(String leftUserId){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "leftUserId");
        jsonObject.addProperty("userId", leftUserId);

        sendMessage(jsonObject);
    }

    /**
     * 通知加入房间的用户的标识
     * @param joinUserId
     */
    public void notifyJoinUserId(String joinUserId){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "joinUserId");
        jsonObject.addProperty("userId", joinUserId);

        sendMessage(jsonObject);
    }

    /**
     * 通知有成员申请使用摄像头
     * @param applyUserId
     */
    public void notifyApplyForHost(String applyUserId){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "receiveApply");
        jsonObject.addProperty("userId", applyUserId);

        sendMessage(jsonObject);
    }

    /**
     * 通知摄像头申请被拒绝
     */
    public void notifyApplyRefused(){
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("id", "applyRefused");

        sendMessage(jsonObject);
    }

}
