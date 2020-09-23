"use strict"
var website = window.website || {};
(function(){
	// https://stackoverflow.com/questions/14521108/dynamically-load-js-inside-js
	website["script"] = function( url, readyFn ){
		if(Array.isArray(url)) {
			for(var i = 0, len = url.length; i < len; i++) {
				var _url = url[i];
				loadScript(_url);
			}
		} else {
			loadScript(url);
		}
		function loadScript(url) {
			var scriptTag = document.createElement('script');
			scriptTag.src = url;
			scriptTag.onload = readyFn;
			scriptTag.onreadystatechange = readyFn;
			document.head.appendChild(scriptTag);
		}
	};
})();
(function(){
	// 新增腳本時於此處增加腳本路徑
    var arr = [];
    (function(){
		arr.push("/testasync/js/jquery/jquery.slim.min.js");
		arr.push("/testasync/js/axios/axios.min.js");
		arr.push("/testasync/js/module/public/public.js");
		arr.push("/testasync/js/module/index/index.js");
	})();
	// 遞迴方式讀取所有 script 項目
	(function(){
		var tmpPath = null;
		loadNext();
		function loadNext() {
			tmpPath = arr.shift();
			if(null != tmpPath) {
				website.script(tmpPath, function(){
					loadNext();
				});
			} else {
				scriptReady();
			}
		}
	})();
	// all script ready
	function scriptReady() {
		// 公用腳本準備完成
		if(null == website.ready) {
			console.error("該頁面不具有 window.website.ready 方法，無法完成初始化呼叫");
		} else {
			// 藉由 setTimeout 將執行優先度降低
			setTimeout(function(){
				website.ready();
			}, 100);
		}
	}
})();
