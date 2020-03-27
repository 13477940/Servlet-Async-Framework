"use strict"
var website = window.website || {};
// 擴充項目
(function(){
	// 亂數產生器
	(function(){
		website["randomString"] = function(len){
			var t = "abcdefghijklmnopqrstuvwxyz0123456789";
			var r = [];
			var l = 16; // default
			var tLen = t.length;
			if(null != len) l = len;
			for(var i = 0; i < l; i++) {
				var c = t.charAt( Math.floor( Math.random() * tLen ) );
				r.push(c);
			}
			return r.join("");
		};
	})();
	// 網址導向腳本
	(function(){
	    website["redirect"] = function(url, onCache){
	        if(null == onCache) onCache = true; // GET 預設是快取狀態
	        var targetURL = null;
	        if(null == url) {
	            targetURL = location.protocol + '//' + location.host + location.pathname;
	        } else {
	            targetURL = url;
	        }
	        if(onCache) {
	            // 網址不變動時 GET 可被快取
	            location.href = targetURL;
	        } else {
	            // 藉由參數取消瀏覽器快取機制（後端要配合）
	            var ts = Date.now();
				if(-1 != targetURL.indexOf("?")) {
					location.href = targetURL + "&ei=" + getRandomString();
				} else {
					location.href = targetURL + "?ei=" + getRandomString();
				}
	        }
	    };
		function getRandomString() {
			return website.randomString();
		}
	})();
	// AJAX - GET x-www-form-urlencoded
	(function(){
		website["get"] = function(reqObj){
			var def = $.Deferred();
			if(null != reqObj["data"] && false == Array.isArray(reqObj["data"])) {
				def.reject();
				console.error("data 參數必須使用 array 型態");
				return;
			}
			var worker = new Worker("/testasync/js/worker/web_worker.js");
			// if worker init error
			worker.addEventListener("error", function(e){
				console.log(e);
			}, false);
			// set listener
			worker.addEventListener("message", workerMsgFn, false);
			function workerMsgFn(e) {
				// console.log(e.data);
				def.resolve(e.data.data);
				worker.terminate();
			}
			// send init msg
			(function(){
				worker.postMessage({
					reqObj: {
						act: "get",
						url: reqObj["url"],
						data: reqObj["data"]
					}
				});
			})();
			return def;
		};
	})();
	// AJAX - POST x-www-form-urlencoded
	(function(){
		website["post"] = function(reqObj){
			var def = $.Deferred();
			if(null != reqObj["data"] && false == Array.isArray(reqObj["data"])) {
				def.reject();
				console.error("data 參數必須使用 array 型態");
				return;
			}
			var worker = new Worker("/testasync/js/worker/web_worker.js");
			// if worker init error
			worker.addEventListener("error", function(e){
				console.log(e);
			}, false);
			// set listener
			worker.addEventListener("message", workerMsgFn, false);
			function workerMsgFn(e) {
				// console.log(e.data);
				def.resolve(e.data.data);
				worker.terminate();
			}
			// send init msg
			(function(){
				worker.postMessage({
					reqObj: {
						act: "post",
						url: reqObj["url"],
						data: reqObj["data"]
					}
				});
			})();
			return def;
		};
	})();
	// AJAX - POST Form Data Multipart
	(function(){
		website["post_form_data"] = function(reqObj){
			var def = $.Deferred();
			if(null != reqObj["data"] && false == Array.isArray(reqObj["data"])) {
				def.reject();
				console.error("data 參數必須使用 array 型態");
				return;
			}
			var worker = new Worker("/testasync/js/worker/web_worker.js");
			// if worker init error
			worker.addEventListener("error", function(e){
				console.log(e);
			}, false);
			// set listener
			worker.addEventListener("message", workerMsgFn, false);
			function workerMsgFn(e) {
				var respObj = e.data;
				if("upload_progress" == respObj["status"]) {
					def.notify(respObj["progress_value"]);
				}
				if("done" == respObj["status"]) {
					def.resolve(e.data.data);
					worker.terminate();
				}
			}
			// send init msg
			(function(){
				worker.postMessage({
					reqObj: {
						act: "post_form_data",
						url: reqObj["url"],
						data: reqObj["data"]
					}
				});
			})();
			return def;
		};
	})();
	// Dialog - implement by pure js
	(function(){
		website["dialog"] = function(initObj) {
			var def = $.Deferred();
			var dialogId = website.randomString(16);
			var dialogElem = $(buildDialogHtml());
			(function(){
				dialogElem.attr("modal_dialog_ssid", dialogId);
				dialogElem.css("z-index", "10");
				dialogElem.css("background-color", "rgba(245,245,245,0.5)");
				if(null != initObj["content"] && initObj["content"].length > 0) {
					dialogElem.find("div[modal_dialog_key=wrap]").append(initObj["content"]);
				}
			})();
			$("body").append(dialogElem);
			dialogElem.css("display", "flex");
			def.resolve(dialogElem);
			return def;
		};
		function buildDialogHtml() {
			var tmp = [];
			tmp.push("<div modal_dialog_key='overlay' style='display: none;position: fixed;top: 0px;left: 0px;height: 100vh;width: 100vw;overflow: auto;'>");
			tmp.push("<div modal_dialog_key='wrap' style='margin: auto'></div>"); // 垂直與水平置中
			tmp.push("</div>");
			return tmp.join('');
		}
	})();
})();
