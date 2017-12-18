/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package cn.superid;

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
 *
 * @author Ivan Gracia (izanmail@gmail.com)
 * @since 4.3.1
 */
public class UserSession implements Closeable {

    private final String name;
    private final String roomName;
    private final WebSocketSession session;
    private final MediaPipeline pipeline;

    private final WebRtcEndpoint outgoingMedia;
    private final ConcurrentMap<String, WebRtcEndpoint> incomingMedia = new ConcurrentHashMap<>();


    public UserSession(final String name, String roomName, final WebSocketSession session,
                       MediaPipeline pipeline) {
        this.name = name;
        this.roomName = roomName;
        this.session = session;
        this.pipeline = pipeline;

        this.outgoingMedia = new WebRtcEndpoint.Builder(pipeline).build();

        this.outgoingMedia.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

            @Override
            public void onEvent(IceCandidateFoundEvent event) {
                JsonObject response = new JsonObject();
                response.addProperty("id", "iceCandidate");
                response.addProperty("name", name);
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

    public String getName() {
        return name;
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
        scParams.addProperty("name", sender.getName());
        scParams.addProperty("sdpAnswer", ipSdpAnswer);

        sendMessage(scParams);
        getEndpointForUser(sender).gatherCandidates();
    }


  private WebRtcEndpoint getEndpointForUser(final UserSession sender) {
    if (sender.getName().equals(name)) {
      return outgoingMedia;
    }


    WebRtcEndpoint incoming = incomingMedia.get(sender.getName());
    if (incoming == null) {
      incoming = new WebRtcEndpoint.Builder(pipeline).build();

      incoming.addIceCandidateFoundListener(new EventListener<IceCandidateFoundEvent>() {

        @Override
        public void onEvent(IceCandidateFoundEvent event) {
          JsonObject response = new JsonObject();
          response.addProperty("id", "iceCandidate");
          response.addProperty("name", sender.getName());
          response.add("candidate", JsonUtils.toJsonObject(event.getCandidate()));
          try {
            synchronized (session) {
              session.sendMessage(new TextMessage(response.toString()));
            }
          } catch (IOException e) {
          }
        }
      });

      incomingMedia.put(sender.getName(), incoming);
    }

    sender.getOutgoingWebRtcPeer().connect(incoming);

    return incoming;
  }





  public void addCandidate(IceCandidate candidate, String name) {
    if (this.name.compareTo(name) == 0) {
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
    boolean eq = name.equals(other.name);
    eq &= roomName.equals(other.roomName);
    return eq;
  }

  @Override
  public int hashCode() {
    int result = 1;
    result = 31 * result + name.hashCode();
    result = 31 * result + roomName.hashCode();
    return result;
  }
}
