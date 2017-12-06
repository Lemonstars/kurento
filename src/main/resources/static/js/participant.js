const PARTICIPANT_MAIN_CLASS = 'participant main';
const PARTICIPANT_CLASS = 'participant';

function Participant(name) {
    this.name = name;
    var container = document.createElement('div');
    container.className = isPresentMainParticipant() ? PARTICIPANT_CLASS : PARTICIPANT_MAIN_CLASS;
    container.id = name;
    var span = document.createElement('span');
    var video = document.createElement('video');
    var rtcPeer;

    container.appendChild(video);
    container.appendChild(span);
    container.onclick = switchContainerClass;
    document.getElementById('participants').appendChild(container);

    span.appendChild(document.createTextNode(name));

    video.id = 'video-'+name;
    video.autoplay = true;
    video.controls = false;

    this.getElement = function(){
        return container
    };

    this.getVideoElement = function () {
        return video;
    };


    function isPresentMainParticipant() {
        return ((document.getElementsByClassName(PARTICIPANT_MAIN_CLASS)).length != 0);
    }

    function switchContainerClass() {
        if (container.className === PARTICIPANT_CLASS) {
            var elements = Array.prototype.slice.call(document.getElementsByClassName(PARTICIPANT_MAIN_CLASS));
            elements.forEach(function(item) {
                item.className = PARTICIPANT_CLASS;
            });

            container.className = PARTICIPANT_MAIN_CLASS;
        } else {
            container.className = PARTICIPANT_CLASS;
        }
    }

    this.onIceCandidate = function (candidate, wp) {
        var  message = {
            id: 'onIceCnadidate',
            candidate: candidate,
            name: name
        };
        sendMessage(message);
    };

    this.offerToReceiveVideo = function(error, offerSdp, wp){
        if (error) return console.error ("sdp offer error");
        var msg =  { id : "receiveVideoFrom",
            sender : name,
            sdpOffer : offerSdp
        };
        sendMessage(msg);
    }

    Object.defineProperty(this, 'rtcPeer', { writable: true});
}