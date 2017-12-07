var ws = new WebSocket('wss://' + location.host + '/groupCall');
var participants = {};
var name;

window.onbeforeunload = function () {
    ws.close();
};

ws.onmessage = function (message) {
    var parseMessage = JSON.parse(message.data);

    switch (parseMessage.id){
        case 'existingParticipants':
            onExistingParticipants(parseMessage);
            break;
        case 'newParticipantArrived':
            onNewParticipant(parseMessage);
            break;
        case 'iceCandidate':
            participants[parseMessage.name].rtcPeer.addIceCandidate(parseMessage.candidate, function (error) {
                if (error) {
                    console.error("Error adding candidate: " + error);
                    return;
                }
            });
            break;

        case 'videoExist':
            console.log("You are chatting on the other video");
            break;
        case 'captureRoomId':
            console.log("roomId: " + parseMessage.name);
            break;
    }
};

function register() {
    name = document.getElementById('name').value;

    document.getElementById('room-header').innerText = 'ROOM ' + room;
    document.getElementById('join').style.display = 'none';
    document.getElementById('room').style.display = 'block';

    var message = {
        id: 'joinNewRoom',
        userId: name
    };

    sendMessage(message)
}

function onNewParticipant(request) {
    receiveVideo(request.name)
}

function onExistingParticipants(msg) {
    var constraints = {
        audio: true,
        video: {
            mandatory:{
                maxWidth: 320,
                maxFrameRate: 15,
                minFrameRate: 15
            }
        }
    };

    var participant = new Participant(name);
    participants[name] = participant;
    var video = participant.getVideoElement();

    var options = {
        localVideo: video,
        mediaConstrains: constraints,
        onicecandidate: participant.onIceCandidate.bind(participant)
    };

    participant.rtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendonly(options,
        function (error) {
        if(error){
            return console.error(error);
        }
        this.generateOffer(participant.offerToReceiveVideo.bind(participant));
    });

    msg.data.forEach(receiveVideo);
}

function sendMessage(message) {
    var jsonMessage = JSON.stringify(message);
    ws.send(jsonMessage);
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
        if(error){
            return console.error(error);
        }
        this.generateOffer(participant.offerToReceiveVideo.bind(participant))
    })

}

