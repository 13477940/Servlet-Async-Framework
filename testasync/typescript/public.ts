// namespace define
var website: any = window.website || {};
var $: any = window.$ || null;
var axios: any = window.axios || null;

// script loader
(function(){
    website["script"] = function(url: string, readyFn: any) {
        if( Array.isArray(url) ) {
            for (var i = 0, len = url.length; i < len; i++) {
                var _url = url[i];
                loadScript(_url);
            }
        } else {
            loadScript(url);
        }
        function loadScript(url: string) {
            var scriptTag = document.createElement('script');
            scriptTag.src = url;
            scriptTag.onload = readyFn;
            document.head.appendChild(scriptTag);
        }
    };
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
    (function () {
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

// Dialog
(function(){
    website["dialog"] = function(initObj: any) {
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
        var dialogObj = {
            dialog: dialogElem.find("div[modal_dialog_key=wrap]"),
            overlay: dialogElem,
            close: function(){
                dialogElem.remove();
            }
        }
        def.resolve(dialogObj);
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

// Dropdown：beta
(function(){
    website["dropdown"] = function(elem: any, opt_arr: any) {
        var def = $.Deferred();
        var target = null;
        var ssid = website.randomString(12);
        (function(){
            var tmp = [];
            tmp.push("<div dropdown_ssid='"+ssid+"' ui_type='dropdown'>");
            tmp.push("<div ui_type='dropdown_label' style='position: relative;z-index: 2;'>dropdown label</div>");
            tmp.push("<div ui_type='dropdown_list' style='display: none;position: absolute;max-height: 400px;'></div>");
            tmp.push("</div>");
            target = $(tmp.join(""));
        })();
        var dd_list = target.find("div[ui_type=dropdown_list]");
        dd_list.css("z-index", "3");
        (function(){
            if(null != opt_arr) {
                for(var i = 0, len = opt_arr.length; i < len; i++) {
                    dd_list.append(opt_arr[i]);
                }
            }
        })();
        var dd_lable = target.find("div[ui_type=dropdown_label]");
        var open_status = false;
        var overlay = $("<div dropdown_overlay='"+ssid+"' style='position: fixed;top: 0px;left: 0px;z-index: 1;width: 100vw;height: 100vh;'></div>");
        dd_lable.on("click", function(){
            if(open_status) {
                closeDropdown();
            } else {
                // dd_list.addClass("show");
                dd_list.css("display", "block");
                $("body").append(overlay);
                overlay.on("click", function(){
                    closeDropdown();
                });
                open_status = true;
            }
        });
        function closeDropdown() {
            // dd_list.removeClass("show");
            dd_list.css("display", "none");
            $("div[dropdown_overlay='"+ssid+"']").remove();
            open_status = false;
        }
        (function(){
            elem.append(target);
            var respObj = {
                dropdown: target,
                label: dd_lable,
                list: dd_list
            };
            def.resolve(respObj);
        })();
        return def;
    };
})();

// hyperlinker
(function(){
    website["redirect"] = function(url: string, onCache: boolean) {
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
            location.href = targetURL + "?ei=" + ts;
        }
    };
})();
