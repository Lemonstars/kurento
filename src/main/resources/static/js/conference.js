var ws = new WebSocket('wss://' + location.host + '/groupCall');
var participants = {};
var userId;

window.onbeforeunload = function () {
    ws.close();
};

ws.onmessage = function (message) {
    var parseMessage = JSON.parse(message.data);

    switch (parseMessage.id){
        case 'iceCandidate':
            participants[parseMessage.name].rtcPeer.addIceCandidate(parseMessage.candidate, function (error) {
                if (error) {
                    console.error("Error adding candidate: " + error);
                    return;
                }
            });
            break;
        case 'newParticipantArrived':
            onNewParticipant(parseMessage.name);
        case 'receiveVideoAnswer':
            receiveVideoResponse(parseMessage);
            break;
        case 'existingParticipants':
            onExistingParticipants(parseMessage.data);
            break;
        case 'captureRoomId':
            console.log('roomId:'+ parseMessage.roomId);
            break
    }
};

function createNewRoom() {
    userId = document.getElementById('userId').value;

    var message = {
        id: "createNewRoom",
        userId: userId
    };

    sendMessage(message);
}

function onNewParticipant(sender) {
    receiveVideo(sender.name);
}

function receiveVideoResponse(result) {
    participants[result.name].rtcPeer.processAnswer (result.sdpAnswer, function (error) {
        if (error) return console.error (error);
    });
}

function receiveVideo(sender) {
    var participant = new Participant(sender);
    participants[sender] = participant;
    var video = participant.getVideoElement();

    var options = {
        remoteVideo: video,
        onicecandidate: participant.onIceCandidate.bind(participant)
    };

    participant.rtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options,
        function (error) {
            if(error) {
                return console.error(error);
            }
            this.generateOffer (participant.offerToReceiveVideo.bind(participant));
        });
}

function onExistingParticipants(msg) {
    var constraints = {
        audio : true,
        video : {
            mandatory : {
                maxWidth : 320,
                maxFrameRate : 15,
                minFrameRate : 15
            }
        }
    };


    var participant = new Participant(userId);
    participants[userId] = participant;
    var video = participant.getVideoElement();

    var options = {
        localVideo: video,
        mediaConstraints: constraints,
        onicecandidate: participant.onIceCandidate.bind(participant)
    };

    participant.rtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendonly(options,
        function (error) {
            if(error) {
                return console.error(error);
            }
            this.generateOffer (participant.offerToReceiveVideo.bind(participant));
        });

    msg.data.forEach(receiveVideo);
}

function sendMessage(message) {
    var jsonMessage = JSON.stringify(message);
    ws.send(jsonMessage);
}