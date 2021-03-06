
var stompClient;
var mixVideo;
var webRtcPeer;
var userId;
var roomId;
var isPresenter;

window.onload = function() {
	mixVideo = document.getElementById('mixVideo');
};

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
            isPresenter = true;

            stompClient.subscribe('/topic/chatContent-' + roomId, function (frame) {
                receiveChatContent(JSON.parse(frame.body).data);
            });

            stompClient.subscribe('/topic/leftUserId-' + roomId, function (frame) {
                receiveSomeoneLeft(JSON.parse(frame.body).data);
            });

            stompClient.subscribe('/topic/joinUserId-' + roomId, function (frame) {
                receiveSomeoneJoin(JSON.parse(frame.body).data);
            });

            document.getElementById('room-header').innerText = 'roomId ' + roomId + '\n' + 'userId ' + userId ;

            var options = {
                remoteVideo : mixVideo,
                onicecandidate : onIceCandidate
            };

            webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options, function(error) {
                if (error) return console.error(error);
                webRtcPeer.generateOffer(onOffer);
            });

        });

        stompClient.subscribe('/queue/sdpAnswer-' + userId, function (frame) {
            webRtcPeer.processAnswer(JSON.parse(frame.body).data, function(error) {
                if (error) return console.error(error);
            });
        });

        stompClient.subscribe('/queue/iceCandidate-' + userId, function (frame) {
            webRtcPeer.addIceCandidate(JSON.parse(frame.body).data, function(error) {
                if (error) return console.error('Error adding candidate: ' + error);
            });
        });

        stompClient.subscribe('/queue/receiveApply-' + userId, function (frame) {
            receiveApply(JSON.parse(frame.body).data);
        });

        stompClient.subscribe('/queue/applyRefused-' + userId, function () {
            applyRefused();
        });

        stompClient.subscribe('/queue/applyAccepted-' + userId, function () {
            applyAccept();
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
        isPresenter = false;

        stompClient.subscribe('/queue/sdpAnswer-' + userId, function (frame) {
            webRtcPeer.processAnswer(JSON.parse(frame.body).data, function(error) {
                if (error) return console.error(error);
            });
        });

        stompClient.subscribe('/queue/iceCandidate-' + userId, function (frame) {
            webRtcPeer.addIceCandidate(JSON.parse(frame.body).data, function(error) {
                if (error) return console.error('Error adding candidate: ' + error);
            });
        });

        stompClient.subscribe('/queue/receiveApply-' + userId, function (frame) {
            receiveApply(JSON.parse(frame.body).data);
        });

        stompClient.subscribe('/queue/applyRefused-' + userId, function () {
            applyRefused();
        });

        stompClient.subscribe('/queue/applyAccepted-' + userId, function () {
            applyAccept();
        });

        stompClient.subscribe('/topic/chatContent-' + roomId, function (frame) {
            receiveChatContent(JSON.parse(frame.body).data);
        });

        stompClient.subscribe('/topic/leftUserId-' + roomId, function (frame) {
            receiveSomeoneLeft(JSON.parse(frame.body).data);
        });

        stompClient.subscribe('/topic/joinUserId-' + roomId, function (frame) {
            receiveSomeoneJoin(JSON.parse(frame.body).data);
        });

        var options = {
            remoteVideo : mixVideo,
            onicecandidate : onIceCandidate
        };

        webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options, function(error) {
            if (error) return console.error(error);
            webRtcPeer.generateOffer(onOffer);
        });

    });

}

function onIceCandidate(candidate) {
    console.log('Local candidate' + JSON.stringify(candidate));

    stompClient.send('/app/onIceCandidate/' + userId, {}, JSON.stringify(candidate));
}

function onOffer(error, offerSdp) {
    if (error) return console.error('Error generating the offer');
    console.info('Invoking SDP offer callback function ' + location.host);
    var message = {
        sdpOffer : offerSdp,
        userId: userId,
        roomId: roomId,
        isPresenter: isPresenter
    };

    stompClient.send('/app/joinRoom', null, JSON.stringify(message));
}


function leaveRoom() {

    document.getElementById('create').style.display = 'block';
    document.getElementById('join').style.display = 'block';
    document.getElementById('room').style.display = 'none';

    stompClient.send('/app/leaveRoom/' + userId, null, null)
}

function obtainChatContent() {
    var chatContent = document.getElementById('chatText').value;
    document.getElementById('chatText').value = '';

    var message = {
        userId: userId,
        roomId: roomId,
        chatContent: chatContent
    };

    stompClient.send('/app/chatSend', null, JSON.stringify(message));
}


function receiveChatContent(message) {
    var content = message.chatContent;
    var senderId = message.userId;

    var chatReceiveDiv = document.getElementById('chatReceiveContent');
    chatReceiveDiv.innerText += senderId +" : "+content+"\n";
}

function applyForHost() {
    document.getElementById('applyForHost').style.display = 'none';

    stompClient.send('/app/applyForHost/'+userId, null, null);
}

function receiveApply(applyId) {
    var receiveApplyDiv = document.getElementById('receiveApply');
    receiveApplyDiv.style.display = 'block';

    var applyInfoDiv = document.getElementById('applyInfo');
    applyInfoDiv.value = applyId;
    applyInfoDiv.innerText += applyId +" : apply for the host\n";
}

function refuseApply() {
    document.getElementById('receiveApply').style.display = 'none';
    var applyUserId = document.getElementById('applyInfo').value;
    stompClient.send('/app/refuseApply/' + applyUserId, null ,null);

    document.getElementById('applyInfo').innerText += 'Your refuse that User' +
        applyUserId +' uses the camera \n';
}

function acceptApply() {
    document.getElementById('receiveApply').style.display = 'none';

    var applyUserId = document.getElementById('applyInfo').value;
    var message = {
        presenterId: userId,
        applierId: applyUserId
    };

    stompClient.send('/app/acceptApply', null, JSON.stringify(message));

    document.getElementById('applyInfo').innerText += 'Your accept that User' +
        applyUserId +'users the camera \n';
    document.getElementById('applyForHost').style.display = 'block';
}

function applyRefused() {
    document.getElementById('applyForHost').style.display = 'block';
    document.getElementById('applyInfo').innerText += 'Your apply is refused \n';
}

function applyAccept() {
    document.getElementById('applyInfo').innerText += 'Your apply is accepted \n';
}

function receiveSomeoneLeft(leftUserId) {
    var chatReceiveDiv = document.getElementById('chatReceiveContent');
    chatReceiveDiv.innerText += 'user ' + leftUserId + ' leave the room\n';
}

function receiveSomeoneJoin(joinUserId) {
    var chatReceiveDiv = document.getElementById('chatReceiveContent');
    chatReceiveDiv.innerText += 'user ' + joinUserId + ' join the room\n';
}



