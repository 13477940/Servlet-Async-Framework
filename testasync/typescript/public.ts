// namespace define
var website: any = window.website || {};
var $: any = window.$ || null;
var axios: any = window.axios || null;

// script loader
(function(){
    website["script"] = function(url: string, readyFn: any) {
        // 檢查是否為陣列格式
        if( Array.isArray(url) ) {
            for (var i = 0, len = url.length; i < len; i++) {
                var _url = url[i];
                loadScript(_url);
            }
        } else {
            loadScript(url);
        }
        // 動態讀取腳本實作
        function loadScript(url: string) {
            rm_rep_script(url);
            append_load_script(url);
        }
        // https://developer.mozilla.org/zh-TW/docs/Web/API/Document/createElement
        // https://developer.mozilla.org/zh-TW/docs/Web/API/Document/readyState
        // https://developer.mozilla.org/en-US/docs/Web/API/Node/appendChild
        function append_load_script(url: any) {
            var timer = setInterval(exec_load_fn, 1);
            // 載入腳本實作
            function exec_load_fn() {
                // 如果沒有在這個時候載入會導致 log cat 無法接收所有此時載入的腳本錯誤 exception
                if("complete" == document.readyState) {
                    var scriptTag = document.createElement('script');
                    scriptTag.src = url;
                    scriptTag.onload = readyFn;
                    scriptTag.onerror = loadErrorFn;
                    document.body.appendChild(scriptTag);
                    clearInterval(timer);
                }
            }
            // 當腳本讀取發生錯誤時
            function loadErrorFn(oError: any) {
                clearInterval(timer);
                console.log(oError);
            }
        }
    };
    // https://developer.mozilla.org/en-US/docs/Web/API/Document/querySelectorAll
    // 當重複載入同個 script 路徑則將舊的 tag 刪除
    function rm_rep_script(url: string) {
        var elems = document.querySelectorAll("script");
        var prefix = window.location.protocol + "//" + window.location.hostname;
        var chk_str = prefix + url;
        for(var i = 0, len = elems.length; i < len; i++) {
            var elem = elems[i];
            if(elem.src.indexOf(chk_str) > -1) {
                elem.remove();
            }
        }
    }
})();

// 亂數產生器
(function(){
    website["randomString"] = function(len: any) {
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

// axios module
(function(){
    (function(){
        website["get"] = function(reqObj: any) {
            var def = $.Deferred();
            if (null != reqObj["header"] && false == Array.isArray(reqObj["header"])) {
                console.error("header 參數必須使用 array 型態");
                def.reject();
                return;
            }
            var headers: any = {};
            (function(){
                var arr = reqObj["header"];
                for (var index in arr) {
                    var obj = arr[index];
                    var key = obj["key"];
                    var value = obj["value"];
                    headers[key] = value;
                }
            })();
            if (null != reqObj["data"] && false == Array.isArray(reqObj["data"])) {
                console.error("data 參數必須使用 array 型態");
                def.reject();
                return;
            }
            var params = new URLSearchParams();
            (function(){
                var arr = reqObj["data"];
                for (var index in arr) {
                    var obj = arr[index];
                    var key = obj["key"];
                    var value = obj["value"];
                    params.append(key, value);
                }
            })();
            var config = {
                transformResponse: [
                    function (data: any) { return data; }
                ],
                headers: headers,
                params: params
            };
            axios.get(reqObj["url"], config).then(function(response: any) {
                var respObj = {
                    status: "done",
                    status_code: response.status,
                    data: response.data
                };
                def.resolve(respObj.data);
            }).catch(function(err: any) {
                def.reject(err);
            });
            return def;
        };
    })();
    (function(){
        website["post"] = function(reqObj: any) {
            var def = $.Deferred();
            if (null != reqObj["header"] && false == Array.isArray(reqObj["header"])) {
                console.error("header 參數必須使用 array 型態");
                def.reject();
                return;
            }
            var headers: any = {};
            (function(){
                var arr = reqObj["header"];
                for (var index in arr) {
                    var obj = arr[index];
                    var key = obj["key"];
                    var value = obj["value"];
                    headers[key] = value;
                }
            })();
            if (null != reqObj["data"] && false == Array.isArray(reqObj["data"])) {
                console.error("data 參數必須使用 array 型態");
                def.reject();
                return;
            }
            var params = new URLSearchParams();
            (function(){
                var arr = reqObj["data"];
                for (var index in arr) {
                    var obj = arr[index];
                    var key = obj["key"];
                    var value = obj["value"];
                    params.append(key, value);
                }
            })();
            var config = {
                transformResponse: [
                    function(data: any) { return data; }
                ],
                headers: headers
            };
            axios.post(reqObj["url"], params, config).then(function(response: any) {
                var respObj = {
                    status: "done",
                    status_code: response.status,
                    data: response.data
                };
                def.resolve(respObj.data);
            }).catch(function (err: any) {
                def.reject(err);
            });
            return def;
        };
    })();
    (function(){
        website["post_json"] = function(reqObj: any) {
            var def = $.Deferred();
            if (null != reqObj["header"] && false == Array.isArray(reqObj["header"])) {
                console.error("header 參數必須使用 array 型態");
                def.reject();
                return;
            }
            var headers: any = {};
            (function () {
                var arr = reqObj["header"];
                for (var index in arr) {
                    var obj = arr[index];
                    var key = obj["key"];
                    var value = obj["value"];
                    headers[key] = value;
                }
            })();
            var config = {
                transformResponse: [
                    function(data: any) { return data; }
                ],
                headers: headers
            };
            axios.post(reqObj["url"], reqObj["text"]).then(function(response: any) {
                var respObj = {
                    status: "done",
                    status_code: response.status,
                    data: response.data
                };
                def.resolve(respObj.data);
            }).catch(function(err: any) {
                def.reject(err);
            });
            return def;
        };
    })();
    // ajax - post form-data multipart
    (function(){
        website["post_form_data"] = function(reqObj: any) {
            var def = $.Deferred();
            // req header
            if(null != reqObj["header"] && false == Array.isArray(reqObj["header"])) {
                console.error("header 參數必須使用 array 型態");
                def.reject();
                return;
            }
            var headers: any = {};
            (function(){
                var arr = reqObj["header"];
                for(var index in arr) {
                    var obj = arr[index];
                    var key = obj["key"];
                    var value = obj["value"];
                    headers[key] = value;
                }
            })();
            // req params
            if(null != reqObj["data"] && false == Array.isArray(reqObj["data"])) {
                console.error("data 參數必須使用 array 型態");
                def.reject();
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
                transformResponse: [function(data: any){ return data; }], // 修正 response 回傳內容格式
                headers: headers,
                // maxContentLength: 20000000,
                onUploadProgress: function(progressEvent: any) {
                    var percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
                    var percentStr = String(percentCompleted); // percent number
                    var respObj = {
                        status: "upload_progress",
                        progress_value: percentStr
                    };
                    def.notify(respObj);
                }
            };
            // form-data 格式請使用 post method
            axios.post(reqObj["url"], formData, config).then(function(response: any) {
                var respObj = {
                    status: "done",
                    status_code: response.status,
                    data: response.data
                };
                def.resolve(respObj.data);
            }).catch(function(err: any) {
                def.reject(err);
            });
            return def;
        };
    })();
})();

// dialog
// ＃210512 更新 esc 按鍵反應
(function(){
    website["dialog"] = function(initObj: any) {
        var def = $.Deferred();
        var dialogId = website.randomString(16);
        var dialogElem = $(buildDialogHtml());
        (function(){
            dialogElem.attr("modal_dialog_ssid", dialogId);
            dialogElem.css("z-index", "10");
            dialogElem.css("background-color", "rgba(90,90,90,0.5)");
            if(null != initObj["content"] && initObj["content"].length > 0) {
                dialogElem.find("div[modal_dialog_key=wrap]").append(initObj["content"]);
            }
        })();
        $("body").append(dialogElem);
        dialogElem.css("display", "flex");
        var dialogObj = {
            dialog: dialogElem.find("div[modal_dialog_key=wrap]"),
            overlay: dialogElem,
            close: function(){
                dialogElem.remove();
            }
        };
        // esc key setting（要配合 tabindex 設定）
        (function(){
            dialogElem.on("keydown", function(evt: any){
                if(27 == evt.keyCode) {
                    dialogObj.close();
                }
            });
        })();
        def.resolve(dialogObj);
        return def;
    };
    function buildDialogHtml() {
        var tmp = [];
        tmp.push("<div modal_dialog_key='overlay' tabindex='0' style='display: none;position: fixed;top: 0px;left: 0px;height: 100vh;width: 100vw;overflow: auto;'>");
        tmp.push("<div modal_dialog_key='wrap' style='margin: auto'></div>"); // 垂直與水平置中
        tmp.push("</div>");
        return tmp.join('');
    }
})();

// hyperlinker
(function(){
    website["redirect"] = function(url: string, onCache: boolean, ran_str: any) {
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
            // 藉由參數取消瀏覽器快取機制
            var ts = Date.now();
            var _ran_str = ts;
            if(null != ran_str) _ran_str = ran_str;
            location.href = targetURL + "?ei=" + _ran_str;
        }
    };
})();
