if (!window.WebSocket)
	alert("WebSocket not supported by this browser");

var server = new CWebSocket();
var listener = {
	onOpen : function() {
		server.send('websockets are open for communications!');
		$('#input').fadeIn();
	},

	onMessage : function(m) {
		if (m.data) {
			$('#messages').append(makeLi("incoming", m.data));
			var messageBox = $('#messages')[0];
			messageBox.scrollTop = messageBox.scrollHeight - messageBox.clientHeight;
		}
	},

	onClose : function(m) {
		$('#messages').append(makeLi("error", "Connection was closed (" + m.code + "): " + m.reason));
		onDisconnectClicked();
		$('#input').fadeOut();
	}
};
server.addListener(listener);

function makeLi(className, text) {
	var spanText = document.createElement('li');
	spanText.className = className;
	spanText.innerHTML = text;
	return spanText;
}

function onConnectClicked() {
	server.connect($("#ws_uri").val());
	$('#connect').attr("disabled", "disabled")
	$('#disconn').removeAttr("disabled")

}

function onDisconnectClicked() {
	server.close();
	$('#disconn').attr("disabled", "disabled")
	$('#connect').removeAttr("disabled")
	
}

function onSendClicked() { 
	var toSend = $("#input input").val();
	$("#input input").val("");
	$('#messages').append(makeLi("outcoming", toSend));
	server.send(toSend)
}

function onInputKey(event) {
	if( event.keyCode == 13 )
		onSendClicked();
}

function onReady() { 
	$('#connect').on("click", onConnectClicked);
	$('#disconn').on("click", onDisconnectClicked);
	$('#send').on("click", onSendClicked);
	$("#input input").on("keypress", onInputKey);
}

$(onReady)