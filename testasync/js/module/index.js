"use strict"
var siteFn = {};
(function() {
    siteFn["readyFn"] = function() {
		// click upload
        $("#upok").on("click", function() {
			var targetFile = $("#upfile")[0]["files"][0];
			(function(){
				var worker = new Worker("/testasync/js/worker/web_worker.js");
				// set listener
				worker.addEventListener("message", workerMsgFn, false);
				function workerMsgFn(e) {
					console.log(e.data);
					worker.terminate();
				}
				// send init msg
				(function(){
					var reqObj = {
						act: "get",
						url: "/testasync/index",
						data: [
							{ key: "a", value: "100" },
							{ key: "b", value: "ABC" },
							{ key: "c", value: "國字測試" }
						]
					};
					if(null != targetFile) {
						// if need upload file, change to form_data mode
						reqObj["act"] = "form_data";
						reqObj["data"].push({ key: "myfile", value: targetFile });
					}
					worker.postMessage( reqObj );
				})();
			})();
        });
		// jQuery + Axios 實作動態腳本載入模擬模組化
		(function(){
			var def = null;
			var worker = new Worker("/testasync/js/worker/web_worker.js");
			worker.addEventListener("message", workerMsgFn, false);
			function workerMsgFn(e) {
				var tmp = [];
				tmp.push("<script>");
				tmp.push(e.data.data);
				tmp.push("</script>");
				$("body").append(tmp.join(''));
				def.resolve(e.data);
			}
			siteFn["add_module"] = function(url) {
				def = $.Deferred();
				var reqObj = { act: "get", url: url };
				worker.postMessage( reqObj );
				return def;
			};
		})();
		siteFn["add_module"]("/testasync/js/moment/moment.min.js").done(function(){
			siteFn["add_module"]("https://cdn.jsdelivr.net/npm/vue").done(function(){
				var myVue = new Vue({
					data: {
						myValue: { key: "now_moment", value: moment().format("YYYY-MM-DD HH:mm:ss") }
					}
				});
				console.log(myVue.myValue);
			});
		});
    };
}());
$(siteFn["readyFn"]);
