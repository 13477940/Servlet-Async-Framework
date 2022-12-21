// script controller function
(function(){
    // 新增腳本時於此處增加腳本路徑
    var arr: any = [];
    (function(){
        arr.push("/testasync/js/jquery/jquery.min.js");
        arr.push("/testasync/js/axios/axios.min.js");
        arr.push("/testasync/js/index.js");
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
            }, 1);
        }
    }
})();

(function(){
    website.ready = function(){
        var page_key = $("body").attr("page_key");
        switch(page_key) {
            case "index": { page_index(); } break;
        }
    };
    function page_index() {
        // you can do something...
    }
})();
