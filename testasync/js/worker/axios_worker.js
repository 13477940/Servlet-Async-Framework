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
	// get with application/x-www-form-urlencoded;charset=UTF-8
	workerFn["get"] = function(reqObj) {
		if(null != reqObj["data"] && false == Array.isArray(reqObj["data"])) {
			console.error("data 參數必須使用 array 型態");
			return;
		}
		var params = new URLSearchParams();
		(function(){
			var arr = reqObj["data"];
			for(var index in arr) {
				var obj = arr[index];
				var key = obj["key"];
				var value = obj["value"];
				params.append(key, value);
			}
		})();
		var config = {
			transformResponse: [(data) => { return data; }],
			params: params
		};
		axios.get(reqObj["url"], config).then(function (response) {
			var respObj = {
				status: "done",
				status_code: response.status,
				data: response.data
			};
			self.postMessage(respObj);
		}).catch(function (err) {
			var respObj = {
				status: "error",
				error: err
			};
			self.postMessage(respObj);
		});
	};
})();

(function(){
	// post form-data with application/x-www-form-urlencoded;charset=UTF-8
	workerFn["post"] = function(reqObj) {
		if(null != reqObj["data"] && false == Array.isArray(reqObj["data"])) {
			console.error("data 參數必須使用 array 型態");
			return;
		}
		// https://github.com/axios/axios#using-applicationx-www-form-urlencoded-format
		var params = new URLSearchParams();
		(function(){
			var arr = reqObj["data"];
			for(var index in arr) {
				var obj = arr[index];
				var key = obj["key"];
				var value = obj["value"];
				params.append(key, value);
			}
		})();
		var config = {
			transformResponse: [(data) => { return data; }]
		};
		axios.post(reqObj["url"], params, config).then(function(response) {
			var respObj = {
				status: "done",
				status_code: response.status,
				data: response.data
			};
			self.postMessage(respObj);
		}).catch(function (err) {
			var respObj = {
				status: "error",
				error: err
			};
			self.postMessage(respObj);
		});
	};
})();

(function(){
	// post form-data with multipart/form-data
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
				var percentStr = String(percentCompleted); // percent number
				var respObj = {
					status: "upload_progress",
					progress_value: percentStr
				};
				self.postMessage(respObj);
			}
		};
		// form-data 格式請使用 post method
		axios.post(reqObj["url"], formData, config).then(function (response) {
			var respObj = {
				status: "done",
				status_code: response.status,
				data: response.data
			};
			self.postMessage(respObj);
		}).catch(function (err) {
			var respObj = {
				status: "error",
				error: err
			};
			self.postMessage(respObj);
		});
	};
})();
