// 此為前端主程式進入點, modify:2023-08-02

// site function
(function(){
    // set webapp name - 所有腳本中對於 webapp 根路徑請統一修改此處
    website["app_name"] = null;
    (function(){
        // 如果是瀏覽器環境
        if(window.location.protocol.indexOf("http") > -1) {
            const path_name = window.location.pathname;
            const path_arr = path_name.split("/");
            const tmp_arr = [];
            while(1 < path_arr.length) {
                const tmp = path_arr.pop();
                if(null == tmp || 0 == tmp.length) {
                    // 修正像是『www/proj/』其中子路徑為空時的處理
                    if(path_arr.length > 0) continue;
                    break;
                }
                tmp_arr.unshift(tmp);
            }
            website.app_name = "/" + tmp_arr[0];
        } else {
            // APP WebView 環境
            const path_href = window.location.href;
            const path_arr = path_href.split("/");
            path_arr.pop(); // 當下路徑上一層
            website.app_name = path_arr.join("/");
        }
        // 如果為外網網址根目錄並且無 app_name 子路徑時，需要由此直接指定
        if("/undefined" == website.app_name) website.app_name = "/testasync";
    })();
    // 新增腳本時於此處增加腳本路徑
    const arr: any = [];
    (function(){
        // module script - 基礎功能腳本
        arr.push(website["app_name"] + "/js/jquery/jquery.min.js");
        arr.push(website["app_name"] + "/js/axios/axios.min.js");
    })();
    // 遞迴方式讀取所有 script 項目
    (function(){
        let tmpPath = null;
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
            // 藉由 setTimeout 特性將執行優先度降低
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
            case "index": {
                website.script(website["app_name"] + "/js/index.js", function(){
                    website["index_fn"]();
                });
            } break;
        }
    };
})();
