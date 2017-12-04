var webSocket = new WebSocket('wss://' + location.host + '/hello');
var videoInput;
var videoOutput;
var webRtcPeer;

window.onload = function(){
    videoInput = document.getElementById('videoInput');
    videoOutput = document.getElementById('videoOutput');
};

window.onclose = function () {
    webSocket.close();
};

window.onbeforeunload = function() {
    webSocket.close();
};

webSocket.onmessage = function(message) {
    var parsedMessage = JSON.parse(message.data);

    switch (parsedMessage.id) {
        case 'startResponse':
            webRtcPeer.processAnswer(parsedMessage.sdpAnswer, function(error) {
                if (error)
                    return console.error(error);
            });
            break;
        case 'iceCandidate':
            webRtcPeer.addIceCandidate(parsedMessage.candidate, function(error) {
                if (error)
                    return console.error('Error adding candidate: ' + error);
            });
            break;
    }
};

function connectKurento() {
    var options = {
        localVideo : videoInput,
        remoteVideo : videoOutput,
        onicecandidate : onIceCandidate
    };

    webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options,
        function(error) {
            if (error)
                return console.error(error);
            webRtcPeer.generateOffer(onOffer);
        });

}

function onOffer(error, offerSdp) {
    if (error)
        return console.error('Error generating the offer');

    var message = {
        id : 'start',
        sdpOffer : offerSdp
    }
    sendMessage(message);
}

function onIceCandidate(candidate) {
    var message = {
        id : 'onIceCandidate',
        candidate : candidate
    };
    sendMessage(message);
}

function sendMessage(message) {
    var jsonMessage = JSON.stringify(message);
    webSocket.send(jsonMessage);
}
