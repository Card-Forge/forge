// There should be some kind of fallback to Flash-powered sockets (IE 9-, Opera with sockets switched off) 
var CWebSocket = function() {
	var _t = this; 
	var eventListeners = {};
	var eventNames = ["Open", "Message", "Close", "Error"];
	var ws;

	function onOpen()    { callListener.apply("Open", arguments); }
	function onClose()   { callListener.apply("Close", arguments); }
	function onError()   { callListener.apply("Error", arguments); }
	function onMessage() { callListener.apply("Message", arguments); }

	function callListener() {
		var q = eventListeners[this]
		if ( q ) for( var i = 0; i < q.length; i++ ) q[i]['on'+ this].apply(q[i], arguments);
	}

	_t.connect = function(location) {
		ws = new WebSocket("ws://" + location);
		ws.onopen = onOpen;
		ws.onmessage = onMessage;
		ws.onclose = onClose;
		ws.onerror = onError;
	}

	_t.addListener = function(obj) {
		for(var i = 0; i < eventNames.length; i++) {
			var evName = eventNames[i]
			var method = obj["on" + evName];
			if( typeof(method) === 'function') {
				var handlers = eventListeners[evName]
				if( 'undefined' === typeof(handler))
					handlers = eventListeners[evName] = [];
				handlers.push(obj);
			}
		}
	}
	
	_t.getWs = function() { return ws; }
	_t.isOpen = function() { return ws && ws.readyState == ws.OPEN; }
	_t.close = function() { ws && ws.close(); }
	
	
	function _send(message) {
		ws && ws.send(message);
	}
	
	_t.send = function(text) {
		text != null && text.length > 0 && _send(text);
	};
};