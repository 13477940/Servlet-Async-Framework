"use strict"
self.addEventListener("message", workerReady, false);
var workerFn = {};

function workerReady(e) {
	var reqObj = e.data.reqObj;
	switch(reqObj["act"]) {
		case "get": { workerFn["get"](reqObj); } break;
		case "post": { workerFn["post"](reqObj); } break;
		case "post_form_data": { workerFn["post_form_data"](reqObj); } break;
	}
}

// WebSocket Client Example
(function(){
	var socketList = [];
	workerFn["web_socket"] = function() {
		(function(){
			function getRootUri() {
				var ws_protocal = "ws://";
				var ws_port = ":80";
				if(location.protocol.indexOf("https") > -1) {
					ws_protocal = "wss://";
					ws_port = ":443";
				}
				return ws_protocal + location.host + ws_port;
			}
			var web_socket = new WebSocket(ws_uri);
			socketList.push(web_socket);
			web_socket.onopen = function(evt) {
				socketReady();
			};
			web_socket.onmessage = function(evt) {
				var eventObj = JSON.parse(evt.data);
				console.log(eventObj);
			};
			web_socket.onerror = function(evt) {
				reconnectFn();
			};
			web_socket.onclose = function(evt) {
				reconnectFn();
			};
			function socketReady() {
				var sendMsgObj = { "content": "test" };
				web_socket.send(JSON.stringify(sendMsgObj));
			}
			function reconnectFn() {
				var opt = socketList.shift();
				opt.close();
				connectWebSocket();
			}
		})();
	};
})();
