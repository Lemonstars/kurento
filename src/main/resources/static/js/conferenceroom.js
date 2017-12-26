var ws = new WebSocket('wss://' + location.host + '/groupCall');
var mixVideo;
var userId;
var webRtcPeer;

window.onload = function() {
	mixVideo = document.getElementById('mixVideo');
};

window.onbeforeunload = function() {
	ws.close();
};

ws.onmessage = function(message) {
	var parsedMessage = JSON.parse(message.data);
	console.info('Received message: ' + message.data);

	switch (parsedMessage.id) {
        case 'roomId':
            document.getElementById('room-header').innerText = 'ROOM ' + parsedMessage.roomId;
            uploadVideoAndAudio();
            break;
        case 'userState':
            console.log('The user is on anther video');
            document.getElementById('room-header').innerText = 'The user is on anther video';
            break;
        case 'iceCandidate':
            webRtcPeer.addIceCandidate(parsedMessage.candidate, function(error) {
                if (error) return console.error('Error adding candidate: ' + error);
            });
            break;
        case 'startResponse':
            startResponse(parsedMessage);
            break;
        default:
            console.error('Unrecognized message', parsedMessage);
    }
};

function createRoom() {
    userId = document.getElementById('user-create').value;

	document.getElementById('create').style.display = 'none';
    document.getElementById('join').style.display = 'none';
	document.getElementById('room').style.display = 'block';

	var message = {
		id : 'createRoom',
        userId : userId
	};

	sendMessage(message);
}

function joinRoom() {
    userId = document.getElementById('user-join').value;
    var roomId = document.getElementById('room-join').value;

    document.getElementById('room-header').innerText = 'ROOM ' + roomId;
    document.getElementById('create').style.display = 'none';
    document.getElementById('join').style.display = 'none';
    document.getElementById('room').style.display = 'block';

    var message = {
        id : 'joinRoom',
        userId: userId,
        roomId: roomId
    };

    sendMessage(message);
}

function uploadVideoAndAudio() {
    var options = {
        remoteVideo : mixVideo,
        onicecandidate : onIceCandidate
    };

    webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options, function(error) {
    	if (error) return console.error(error);
    	webRtcPeer.generateOffer(onOffer);
	});
}

function onIceCandidate(candidate) {
    console.log('Local candidate' + JSON.stringify(candidate));

    var message = {
        id : 'onIceCandidate',
        candidate : candidate
    };
    sendMessage(message);
}

function onOffer(error, offerSdp) {
    if (error) return console.error('Error generating the offer');
    console.info('Invoking SDP offer callback function ' + location.host);
    var message = {
        id : 'startVideo',
		userId: userId,
        sdpOffer : offerSdp
    };
    sendMessage(message);
}

function startResponse(message) {
    console.log('SDP answer received from server. Processing ...');

    webRtcPeer.processAnswer(message.sdpAnswer, function(error) {
        if (error) return console.error(error);
    });
}

function sendMessage(message) {
	var jsonMessage = JSON.stringify(message);
	console.log('Senging message: ' + jsonMessage);
	ws.send(jsonMessage);
}




