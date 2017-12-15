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
            onNewParticipant(parseMessage);
            break;
        case 'receiveVideoAnswer':
            receiveVideoResponse(parseMessage);
            break;
        case 'existingParticipants':
            onExistingParticipants(parseMessage);
            break;
        case 'participantLeft':
            onParticipantLeft(parseMessage);
            break;
        case 'captureRoomId':
            console.log('roomId:'+ parseMessage.roomId);
            break;
        case 'videoExist':
            console.log(parseMessage.userId + 'is on anther video');
            break;
    }
};

function onNewParticipant(sender) {
    receiveVideo(sender.userId);
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

function onParticipantLeft(request) {
    var participant = participants[request.name];
    participant.dispose();
    delete participants[request.name];
}

function sendMessage(message) {
    var jsonMessage = JSON.stringify(message);
    ws.send(jsonMessage);
}


//创建房间
function createNewRoom() {
    userId = document.getElementById('userId').value;

    var message = {
        id: "createRoom",
        userId: userId
    };

    sendMessage(message);
}

//加入已创建的房间
function joinRoom() {
    var viewerId = document.getElementById('userId-join').value;
    var roomId = document.getElementById('roomId-join').value;

    var message = {
        id: "joinRoom",
        userId: viewerId,
        roomId: roomId
    };

    sendMessage(message)
}
