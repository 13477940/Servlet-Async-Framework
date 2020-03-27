/*
https://caniuse.com/#feat=webworkers
https://github.com/axios/axios
*/

"use strict"
self.importScripts("/testasync/js/axios/axios.min.js");
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

(function(){
	workerFn["get"] = function(reqObj) {
		if(null != reqObj["data"] && false == Array.isArray(reqObj["data"])) {
			console.error("data 參數必須使用 array 型態");
			return;
		}
		var data = {};
		(function(){
			var arr = reqObj["data"];
			for(var index in arr) {
				var obj = arr[index];
				var key = obj["key"];
				var value = obj["value"];
				data[key] = value;
			}
		})();
		var axiosObj = {
			transformResponse: [(data) => { return data; }],
			method: "get",
			url: reqObj["url"],
			params: data
		};
		axios(axiosObj).then(function (response) {
			// console.log(response);
			var respObj = {
				req_type: "get",
				status: "done",
				status_code: response.status,
				status_text: response.statusText,
				data: response.data,
				headers: response.headers
				// config: response.config
				// request: response.request
			};
			self.postMessage(respObj);
		}).catch(function (err) {
			var respObj = {
				req_type: "get",
				status: "error",
				error: err
			};
			self.postMessage(respObj);
		});
	};
})();

(function(){
	workerFn["post"] = function(reqObj) {
		if(null != reqObj["data"] && false == Array.isArray(reqObj["data"])) {
			console.error("data 參數必須使用 array 型態");
			return;
		}
		var data = {};
		(function(){
			var arr = reqObj["data"];
			for(var index in arr) {
				var obj = arr[index];
				var key = obj["key"];
				var value = obj["value"];
				data[key] = value;
			}
		})();
		var axiosObj = {
			transformResponse: [(data) => { return data; }],
			method: "post",
			url: reqObj["url"],
			params: data
		};
		axios(axiosObj).then(function(response) {
			// console.log(response);
			var respObj = {
				req_type: "post",
				status: "done",
				status_code: response.status,
				status_text: response.statusText,
				data: response.data,
				headers: response.headers
				// config: response.config
				// request: response.request
			};
			self.postMessage(respObj);
		}).catch(function(err) {
			var respObj = {
				req_type: "post",
				status: "error",
				error: err
			};
			self.postMessage(respObj);
		});
	};
})();

(function(){
	workerFn["post_form_data"] = function(reqObj) {
		if(null != reqObj["data"] && false == Array.isArray(reqObj["data"])) {
			console.error("data 參數必須使用 array 型態");
			return;
		}
		var formData = new FormData();
		(function(){
			if(null != reqObj["data"]) {
				var arr = reqObj["data"];
				for(var index in arr) {
					var obj = arr[index];
					formData.append(obj["key"], obj["value"]);
				}
			}
		})();
		var config = {
			transformResponse: [(data) => { return data; }],
			// maxContentLength: 20000000,
			onUploadProgress: function(progressEvent) {
				var percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
				var percentStr = String(percentCompleted);
				// console.log("uploadPercent:"+percentStr+"%");
				var respObj = {
					req_type: "post_form_data",
					status: "upload_progress",
					progress_value: percentStr
				};
				self.postMessage(respObj);
			}
		};
		// 要注意請使用 axios.post()
		axios.post(reqObj["url"], formData, config).then(function (response) {
			// console.log(response);
			var respObj = {
				req_type: "post_form_data",
				status: "done",
				status_code: response.status,
				status_text: response.statusText,
				data: response.data,
				headers: response.headers
				// config: response.config
				// request: response.request
			};
			self.postMessage(respObj);
		}).catch(function (err) {
			var respObj = {
				req_type: "post_form_data",
				status: "error",
				error: err
			};
			self.postMessage(respObj);
		});
	};
})();

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
