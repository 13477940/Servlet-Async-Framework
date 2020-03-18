"use strict"
var testasync = {};
(function() {
    testasync["readyFn"] = function() {
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
    };
}());
$(testasync["readyFn"]);
