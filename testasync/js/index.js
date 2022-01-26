"use strict";
var website = window.website || {};
(function(){
    // websocket client
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
        var socketList = [];
        var ws_uri = getRootUri() + "/testasync/websocket";
        var web_socket = new WebSocket(ws_uri);
        socketList.push(web_socket);
        web_socket.onopen = function(evt) {
            // console.log(evt);
            socketReady();
        };
        web_socket.onmessage = function(evt) {
            var eventObj = JSON.parse(evt.data);
            // sendMsg(eventObj);
            console.log(eventObj);
        };
        web_socket.onerror = function(evt) {
            // console.log(evt);
            reconnectFn();
        };
        web_socket.onclose = function(evt) {
            // console.log(evt);
            reconnectFn();
        };
        function socketReady() {
            var tmpObj = { "cmd": "viewer_reg" };
            web_socket.send(JSON.stringify(tmpObj));
        }
        function reconnectFn() {
            setTimeout(function(){
                var opt = socketList.shift();
                opt.close();
                // initWebSocket();
            }, 10000); // 10s
        }
    })();
})();
