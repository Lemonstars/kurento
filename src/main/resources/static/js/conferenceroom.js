
var stompClient;
var mixVideo;
var webRtcPeer;
var userId;
var roomId;

window.onload = function() {
	mixVideo = document.getElementById('mixVideo');
};


// ws.onmessage = function(message) {
// 	var parsedMessage = JSON.parse(message.data);
// 	console.info('Received message: ' + message.data);
//
// 	switch (parsedMessage.id) {
//         case 'chatContent':
//             receiveChatContent(parsedMessage);
//             break;
//         case 'leftUserId':
//             receiveSomeoneLeft(parsedMessage);
//             break;
//         case 'joinUserId':
//             receiveSomeoneJoin(parsedMessage);
//             break;
//         case 'receiveApply':
//             receiveApply(parsedMessage);
//             break;
//         case 'applyRefused':
//             applyRefused();
//             break;
//         default:
//             console.error('Unrecognized message', parsedMessage);
//     }
// };

function createRoom() {
    userId = document.getElementById('user-create').value;

    document.getElementById('room-header').innerText = 'roomId ' + roomId + '\n' + 'userId ' + userId ;
	document.getElementById('create').style.display = 'none';
    document.getElementById('join').style.display = 'none';
	document.getElementById('room').style.display = 'block';

    var socket = new SockJS('/kurento');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function (frame) {

        stompClient.subscribe('/queue/roomId-' + userId, function (frame) {
            roomId = JSON.parse(frame.body).data;

            document.getElementById('room-header').innerText = 'roomId ' + roomId + '\n' + 'userId ' + userId ;
            uploadVideoAndAudio();
        });

        stompClient.subscribe('/queue/startResponse-' + userId, function (frame) {
            webRtcPeer.processAnswer(frame.body, function(error) {
                if (error) return console.error(error);
            });
        });

        stompClient.subscribe('/queue/iceCandidate-' + userId, function (frame) {
            webRtcPeer.addIceCandidate(JSON.parse(frame.body), function(error) {
                if (error) return console.error('Error adding candidate: ' + error);
            });
        });

        stompClient.send('/app/createRoom/' + userId, {},  null);
    });

}

function joinRoom() {
    userId = document.getElementById('user-join').value;
    roomId = document.getElementById('room-join').value;

    document.getElementById('room-header').innerText = 'roomId ' + roomId + '\n' + 'userId ' + userId ;
    document.getElementById('create').style.display = 'none';
    document.getElementById('join').style.display = 'none';
    document.getElementById('room').style.display = 'block';

    document.getElementById('applyForHost').style.display = 'block';


    var socket = new SockJS('/kurento');
    stompClient = Stomp.over(socket);
    stompClient.connect({}, function (frame) {

        stompClient.subscribe('/queue/startResponse-' + userId, function (frame) {
            webRtcPeer.processAnswer(frame.body, function(error) {
                if (error) return console.error(error);
            });
        });

        stompClient.subscribe('/queue/iceCandidate-' + userId, function (frame) {
            webRtcPeer.addIceCandidate(JSON.parse(frame.body), function(error) {
                if (error) return console.error('Error adding candidate: ' + error);
            });
        });

        var options = {
            remoteVideo : mixVideo,
            onicecandidate : onIceCandidate
        };

        webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options, function(error) {
            if (error) return console.error(error);
            webRtcPeer.generateOffer(onOfferJoin);
        });

    });


}

function uploadVideoAndAudio() {
    var options = {
        remoteVideo : mixVideo,
        onicecandidate : onIceCandidate
    };

    webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options, function(error) {
    	if (error) return console.error(error);
    	webRtcPeer.generateOffer(onOfferStart);
	});
}

function onIceCandidate(candidate) {
    console.log('Local candidate' + JSON.stringify(candidate));

    stompClient.send('/app/onIceCandidate/' + userId, {}, JSON.stringify(candidate));
}

function onOfferStart(error, offerSdp) {
    if (error) return console.error('Error generating the offer');
    console.info('Invoking SDP offer callback function ' + location.host);

    stompClient.send('/app/startVideo/' + userId, null, offerSdp);

}

function onOfferJoin(error, offerSdp) {
    if (error) return console.error('Error generating the offer');
    console.info('Invoking SDP offer callback function ' + location.host);
    var message = {
        sdpOffer : offerSdp,
        userId: userId,
        roomId: roomId
    };

    stompClient.send('/app/joinVideo', null, JSON.stringify(message));
}


function leaveRoom() {
    var message = {
        id : 'leaveRoom',
        userId: userId
    };

    document.getElementById('create').style.display = 'block';
    document.getElementById('join').style.display = 'block';
    document.getElementById('room').style.display = 'none';

    // sendMessage(message)
}

function obtainChatContent() {
    var chatContent = document.getElementById('chatText').value;
    document.getElementById('chatText').value = '';

    var message = {
        id: 'chatSend',
        userId: userId,
        roomId: roomId,
        content: chatContent
    };

    // sendMessage(message)
}

function applyForHost() {
    document.getElementById('applyForHost').style.display = 'none';

    var message = {
        id: 'applyForHost',
        userId: userId
    };

    // sendMessage(message)
}

function receiveApply(message) {
    var applyId = message.userId;

    var receiveApplyDiv = document.getElementById('receiveApply');
    receiveApplyDiv.style.display = 'block';

    var applyInfoDiv = document.getElementById('applyInfo');
    applyInfoDiv.value = applyId;
    applyInfoDiv.innerText += applyId +" : apply for the host\n";
}

function refuseApply() {
    document.getElementById('receiveApply').style.display = 'none';

    var applyUserId = document.getElementById('applyInfo').value;
    var message = {
        id: 'refuseApply',
        applyUserId: applyUserId
    };
    // sendMessage(message);
}

function acceptApply() {
    document.getElementById('receiveApply').style.display = 'none';

    var applyUserId = document.getElementById('applyInfo').value;
    var message = {
        id: 'acceptApply',
        userId: userId,
        applyUserId: applyUserId
    };
    // sendMessage(message);
}

function applyRefused() {
    document.getElementById('applyForHost').style.display = 'block';
    document.getElementById('applyInfo').innerText += 'Your apply is refused \n';
}

function receiveChatContent(message) {
    var content = message.content;
    var senderId = message.senderId;

    var chatReceiveDiv = document.getElementById('chatReceiveContent');
    chatReceiveDiv.innerText += senderId +" : "+content+"\n";
}

function receiveSomeoneLeft(message) {
    var leftUserId = message.userId;

    var chatReceiveDiv = document.getElementById('chatReceiveContent');
    chatReceiveDiv.innerText += 'user ' + leftUserId + ' leave the room\n';
}

function receiveSomeoneJoin(message) {
    var joinUserId = message.userId;

    var chatReceiveDiv = document.getElementById('chatReceiveContent');
    chatReceiveDiv.innerText += 'user ' + joinUserId + ' join the room\n';
}



