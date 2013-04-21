
// There should be some kind of fallback to Flash-powered sockets (IE 9-, Opera with sockets switched off) 
var Socket = function() {
	var _t = Observable(["Open", "Message", "Close", "Error"]); 

	function onOpen()    { _t.fireEvent.apply("Open", arguments); }
	function onClose()   { _t.fireEvent.apply("Close", arguments); }
	function onError()   { _t.fireEvent.apply("Error", arguments); }
	function onMessage() { _t.fireEvent.apply("Message", arguments); }

	var ws;
	_t.connect = function(location) {
		ws = new WebSocket("ws://" + location);
		ws.onopen = onOpen;
		ws.onmessage = onMessage;
		ws.onclose = onClose;
		ws.onerror = onError;
	}

//	_t.getWs = function() { return ws; }
	_t.isOpen = function() { return ws && ws.readyState == ws.OPEN; }
	_t.close = function() { ws && ws.close(); }
	_t.send = function(text) { text != null && text.length > 0 && ws && ws.send(text); };

	return _t;
};

