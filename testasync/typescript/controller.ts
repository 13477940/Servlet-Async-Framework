// namespace define
var $: any = window.$ || null;

// site function
(function(){
    // 新增腳本時於此處增加腳本路徑
    var arr: any = [];
    (function(){
        arr.push("/testasync/js/jquery/jquery.min.js");
        arr.push("/testasync/js/axios/axios.min.js");
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
        // ajax worker example
        (function(){
            $("#btn_select_file").on("click", function(){
                $("#upfile").click();
            });
        })();
        (function(){
            $("#label_select_file").html("[未選擇檔案]");
            $("#upfile").on("change", function(){
                var targetFile = $("#upfile")[0]["files"][0];
                if(null != targetFile) {
                    $("#label_select_file").html(targetFile.name);
                } else {
                    $("#label_select_file").html("[未選擇檔案]");
                }
            });
        })();
        (function(){
            $("#btn_upload_submit").on("click", function(){
                var targetFile = $("#upfile")[0]["files"][0];
                (function(){
                    var reqObj = {
                        url: "/testasync/index",
                        data: [
                            { key: "a", value: "100" },
                            { key: "b", value: "ABC" },
                            { key: "c", value: "國字測試" }
                        ]
                    };
                    if(null != targetFile) {
                        reqObj.data.push({
                            key: "myfile",
                            value: targetFile
                        });
                    }
                    website.post_form_data(reqObj)
                        .progress(function(prog: any){
                            console.log(prog);
                        })
                        .done(function(respd: any){
                            console.log(respd);
                        });
                })();
            });
        })();
        // pure javascript dialog example
        // https://www.w3schools.com/howto/howto_css_modals.asp
        (function(){
            function openDialog() {
                website.dialog({
                    content: "<div style='position: relative; width: 400px;height: 400px;margin: auto 0px auto 0px;background-color: #fff;border-radius: 5px;'><span dialog_btn='close' style='position: absolute;top: 20px;right: 20px;'>&times;</span>TEST</div>"
                }).done(function(dialogObj: any){
                    console.log(dialogObj);
                    dialogObj.overlay.css("background-color", "rgba(190,190,190,0.5)");
                    dialogObj.dialog.find("span[dialog_btn=close]").on("click", function(){
                        dialogObj.close();
                    });
                });
            }
            $("button[ui_key=btn_open_dialog]").on("click", function(){
                openDialog();
            });
        })();
        (function(){
            $("button[ui_key=btn_test_get]").on("click", function(){
                website.get({
                    url: "/testasync/index",
                    data: [
                        { key: "a", value: "100" },
                        { key: "b", value: "ABC" },
                        { key: "c", value: "國字測試" },
                        { key: "d", value: "/// //aaa// a//cc[]///" }
                    ],
                    header: [
                        { key: "my-auth", value: "aaabbbcccdddeeefffggg_hi" }
                    ]
                }).done(function(respd: any){
                    console.log(respd);
                });
            });
        })();
        (function(){
            $("button[ui_key=btn_test_post]").on("click", function(){
                website.post({
                    url: "/testasync/index",
                    data: [
                        { key: "a", value: "100" },
                        { key: "b", value: "ABC" },
                        { key: "c", value: "國字測試" },
                        { key: "d", value: "/// //aaa// a//cc[]///" }
                    ]
                }).done(function(respd: any){
                    console.log(respd);
                });
            });
        })();
        (function(){
            function asyncTest() {
                setTimeout(function(){
                    var reqObj = {
                        url: "/testasync/index",
                        data: [
                            { key: "A", value: "100" },
                            { key: "B", value: "200" },
                            { key: "C", value: "300" }
                        ]
                    };
                    website.get(reqObj).done(function(respd: any){
                        var obj = JSON.parse(respd);
                        console.log(obj);
                    });
                }, 1000);
            }
            asyncTest();
            /*
            for(var i = 0, len = 20; i < len; i++) {
                asyncTest();
            }*/
        })();
    }
})();
