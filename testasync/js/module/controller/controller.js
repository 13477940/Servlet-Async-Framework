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
	website.script("/testasync/js/jquery/jquery.slim.min.js", function(){
		website.script("/testasync/js/axios/axios.min.js", function(){
			website.script("/testasync/js/module/public/public.js", function(){
				website.script("/testasync/js/module/index/index.js", function(){
					scriptReady();
				});
			});
		});
	});
	// all script ready
	function scriptReady() {
		if(null == website.ready) {
			console.error("該頁面不具有 window.website.ready 方法，無法完成初始化呼叫");
		} else {
			website.ready();
		}
	}
})();
