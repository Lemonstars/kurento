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
import java.util.Iterator;
import java.util.Set;
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
        user.setHubPort(hubPort);
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

        notifySomeoneJoin(user.getUserId());
    }

    public void changeCameraHost(User currentPresenter, User applier){
        WebRtcEndpoint currentWebRtcEndpoint = currentPresenter.getWebRtcEndpoint();
        HubPort currentHubPort = currentPresenter.getHubPort();
        currentWebRtcEndpoint.disconnect(currentHubPort);

        WebRtcEndpoint applierWebRtcEndpoint = applier.getWebRtcEndpoint();
        HubPort applierHubPort = applier.getHubPort();
        applierWebRtcEndpoint.disconnect(applierHubPort);

        currentWebRtcEndpoint.connect(currentHubPort, MediaType.AUDIO);
        applierWebRtcEndpoint.connect(applierHubPort);

        currentPresenter.setPresenter(false);
        applier.setPresenter(true);
    }

    public void transferChatContent(String content, String senderId){
        Set<String> userIdSet = participants.keySet();
        User receiver;
        for (String userId: userIdSet) {
            receiver = participants.get(userId);
            receiver.notifyChatContent(content, senderId);
        }
    }

    public void notifySomeoneLeft(String leftUserId){
        Set<String> allUserId = participants.keySet();
        User userToNotify;
        for(String userId: allUserId){
            userToNotify = participants.get(userId);
            userToNotify.notifyLeftUserId(leftUserId);
        }
    }

    public void notifySomeoneJoin(String joinUserId){
        Set<String> allUserId = participants.keySet();
        User userToNotify;
        for(String userId: allUserId){
            userToNotify = participants.get(userId);
            userToNotify.notifyJoinUserId(joinUserId);
        }
    }

    public MediaPipeline getPipeline() {
        return pipeline;
    }

    public boolean isRoomEmpty(){
        return participants.isEmpty();
    }

    public void removeUserId(String removeUserId){
        participants.remove(removeUserId);
    }

    public User getPresenter(){
        Set<String> set = participants.keySet();
        Iterator<String> iterable = set.iterator();
        while (iterable.hasNext()){
            String key = iterable.next();
            User user = participants.get(key);
            if(user.isPresenter()){
                return user;
            }
        }
        return null;
    }

    @Override
    public void close() {
        participants.clear();
        pipeline.release();
        outHubPort.release();
        composite.release();

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
