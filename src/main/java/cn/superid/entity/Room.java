package cn.superid.entity;

import com.google.gson.JsonObject;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import javax.annotation.PreDestroy;
import java.io.Closeable;
import java.io.IOException;
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
    private HubPort outHubPort;
    private ConcurrentMap<String, User> participants = new ConcurrentHashMap<>();

    public Room(String roomId, MediaPipeline pipeline) {
        this.roomId = roomId;
        this.pipeline = pipeline;
        this.composite = new Composite.Builder(pipeline).build();
        this.outHubPort = new HubPort.Builder(composite).build();

        log.info("ROOM {} has been created", roomId);
    }

    @PreDestroy
    private void shutdown() {
        this.close();
    }

    public String getRoomId() {
        return roomId;
    }

    public void joinRoom(User user, String sdpOffer, WebSocketSession session){
        WebRtcEndpoint webRtcEndpoint = user.getWebRtcEndpoint();

        String sdpAnswer = webRtcEndpoint.processOffer(sdpOffer);
        JsonObject response = new JsonObject();
        response.addProperty("id", "startResponse");
        response.addProperty("sdpAnswer", sdpAnswer);

        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(response.toString()));
            }
        }catch (IOException e){
            e.printStackTrace();
        }

        webRtcEndpoint.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {
            @Override
            public void onEvent(IceCandidateFoundEvent event) {
                JsonObject response = new JsonObject();
                response.addProperty("id", "iceCandidate");
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
        webRtcEndpoint.gatherCandidates();

        HubPort hubPort = new HubPort.Builder(composite).build();
        webRtcEndpoint.connect(hubPort);
        outHubPort.connect(webRtcEndpoint);
    }

    public MediaPipeline getPipeline() {
        return pipeline;
    }

    @Override
    public void close() {
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
