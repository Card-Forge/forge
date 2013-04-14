if (!window.WebSocket)
	alert("WebSocket not supported by this browser");

var server = new CWebSocket();
var listener = {
	onOpen : function() {
		$('#input').slideDown();
	},

	onMessage : function(m) {
		if (m.data) {
			addLi("incoming", m.data);
		}
	},

	onClose : function(m) {
		addLi("error", "Connection was closed (" + m.code + "): " + m.reason);
		onDisconnectClicked();
		$('#input').fadeOut();
	}
};
server.addListener(listener);

function addLi(className, text) {
	var spanText = document.createElement('li');
	spanText.className = className;
	spanText.innerHTML = text;
	var messageBox = $('#messages')[0];
	messageBox.appendChild(spanText);
	messageBox.scrollTop = messageBox.scrollHeight - messageBox.clientHeight;
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
	addLi("outcoming", toSend);
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