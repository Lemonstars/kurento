package cn.superid.entity;

import cn.superid.util.ResponseUtil;
import org.kurento.client.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.io.Closeable;

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

    public User(String userId, String roomId, boolean isPresenter, MediaPipeline mediaPipeline) {
        this.userId = userId;
        this.roomId = roomId;
        this.isPresenter = isPresenter;

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

    public void addCandidate(IceCandidate candidate) {
        webRtcEndpoint.addIceCandidate(candidate);
    }

    public void processSdpOffer(String sdpOffer, SimpMessagingTemplate messagingTemplate){
        String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);
        messagingTemplate.convertAndSend("/queue/sdpAnswer-" + userId, ResponseUtil.successResponse(sdpAnswer));

        webRtcEndpoint.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {
            @Override
            public void onEvent(IceCandidateFoundEvent event) {
                messagingTemplate.convertAndSend("/queue/iceCandidate-" + userId, ResponseUtil.successResponse(event.getCandidate()));
            }
        });
        webRtcEndpoint.gatherCandidates();
    }

    @Override
    public void close() {
        webRtcEndpoint.release();
    }
}
