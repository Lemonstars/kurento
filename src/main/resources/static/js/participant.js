function Participant(name) {
	this.name = name;
	var container = document.createElement('div');
	container.id = name;
	var span = document.createElement('span');
	var video = document.createElement('video');
	var rtcPeer;

	container.appendChild(video);
	container.appendChild(span);
	document.getElementById('participants').appendChild(container);

	span.appendChild(document.createTextNode(name));

	video.id = 'video-' + name;
	video.autoplay = true;
	video.controls = false;


	this.getElement = function() {
		return container;
	}

	this.getVideoElement = function() {
		return video;
	}

	this.offerToReceiveVideo = function(error, offerSdp, wp){
		if (error) return console.error ("sdp offer error");
		console.log('Invoking SDP offer callback function');
		var msg =  {
			id : "receiveVideoFrom",
			sender : name,
			sdpOffer : offerSdp
		};
		sendMessage(msg);
	};


	this.onIceCandidate = function (candidate, wp) {
		console.log("Local candidate" + JSON.stringify(candidate));

		var message = {
		    id: 'onIceCandidate',
		    candidate: candidate,
		    name: name
		};
		sendMessage(message);
	};

	Object.defineProperty(this, 'rtcPeer', { writable: true});

	this.dispose = function() {
		console.log('Disposing participant ' + this.name);
		this.rtcPeer.dispose();
		container.parentNode.removeChild(container);
	};
}
