package cn.superid.entity;

import org.kurento.client.Composite;
import org.kurento.client.Continuation;
import org.kurento.client.MediaPipeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.io.Closeable;
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


    public String getRoomId() {
        return roomId;
    }


    public void joinRoom(User user){

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
