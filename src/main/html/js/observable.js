var Observable = function(eventNames) { 
	var _t = {};
	var observers = {};

	_t.addObserver = function(obj) {
		for(var i = 0; i < eventNames.length; i++) {
			var evName = eventNames[i]
			var method = obj["on" + evName];
			if( typeof(method) === 'function') {
				var handlers = observers[evName]
				if( 'undefined' === typeof(handler))
					handlers = observers[evName] = [];
				handlers.push(obj);
			}
		}
	}
	
	_t.fireEvent = function() { // usually invoked as .apply(EventName, args)
		var q = observers[this]
		if ( q ) for( var i = 0; i < q.length; i++ ) q[i]['on'+ this].apply(q[i], arguments);
	}
	return _t;
}