package cn.superid.entity;

import com.google.gson.JsonObject;
import org.kurento.client.*;
import org.kurento.jsonrpc.JsonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author 刘兴
 * @date 2017-12-18
 */
public class Room implements Closeable {

    private static final int WAIT_TIME = 5;

    private final Logger log = LoggerFactory.getLogger(Room.class);

    private String roomId;
    private boolean isRecord;
    private HubPort outHubPort;
    private Composite composite;
    private MediaPipeline pipeline;
    private RecorderEndpoint recorderEndpoint;
    private ConcurrentMap<String, User> participants = new ConcurrentHashMap<>();

    public Room(String roomId, MediaPipeline pipeline) {
        this.roomId = roomId;
        this.pipeline = pipeline;
        this.composite = new Composite.Builder(pipeline).build();
        this.outHubPort = new HubPort.Builder(composite).build();
        String recordFilePath = "file:///tmp/kurentoRecordFile/" + roomId + ".webm";
        this.recorderEndpoint = new RecorderEndpoint.Builder(pipeline, recordFilePath).
                withMediaProfile(MediaProfileSpecType.WEBM).build();

        log.info("ROOM {} has been created", roomId);
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
        if(user.isPresenter()){
            webRtcEndpoint.connect(hubPort);
        }else {
            webRtcEndpoint.connect(hubPort, MediaType.AUDIO);
        }
        outHubPort.connect(webRtcEndpoint);

        participants.put(user.getUserId(), user);

        if(!isRecord){
            outHubPort.connect(recorderEndpoint);
            recorderEndpoint.record();
            isRecord = true;
        }

    }

    public MediaPipeline getPipeline() {
        return pipeline;
    }

    public boolean isRoomEmpty(){
        return participants.isEmpty();
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

        if (recorderEndpoint != null) {
            CountDownLatch stoppedCountDown = new CountDownLatch(1);
            ListenerSubscription subscriptionId = recorderEndpoint.addStoppedListener(new EventListener<StoppedEvent>() {
                @Override
                public void onEvent(StoppedEvent event) {
                    stoppedCountDown.countDown();
                }
            });

            recorderEndpoint.stop();

            try {
                if (!stoppedCountDown.await(WAIT_TIME, TimeUnit.SECONDS)) {
                    log.error("Error waiting for recorder to stop");
                }
            } catch (InterruptedException e) {
                log.error("Exception while waiting for state change", e);
            }

            recorderEndpoint.removeStoppedListener(subscriptionId);
        }
    }

}
