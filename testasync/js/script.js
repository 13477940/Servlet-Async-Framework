"use strict";
var website = window.website || {};
var $ = window.$ || null;
var axios = window.axios || null;
(function () {
    website["script"] = function (url, readyFn) {
        if (Array.isArray(url)) {
            for (var i = 0, len = url.length; i < len; i++) {
                var _url = url[i];
                loadScript(_url);
            }
        }
        else {
            loadScript(url);
        }
        function loadScript(url) {
            rm_rep_script(url);
            append_load_script(url);
        }
        function append_load_script(url) {
            var timer = setInterval(exec_load_fn, 1);
            function exec_load_fn() {
                if ("complete" == document.readyState) {
                    var scriptTag = document.createElement('script');
                    scriptTag.src = url;
                    scriptTag.onload = readyFn;
                    scriptTag.onerror = loadErrorFn;
                    document.body.appendChild(scriptTag);
                    clearInterval(timer);
                }
            }
            function loadErrorFn(oError) {
                clearInterval(timer);
                console.log(oError);
            }
        }
    };
    function rm_rep_script(url) {
        var elems = document.querySelectorAll("script");
        var prefix = window.location.protocol + "//" + window.location.hostname;
        var chk_str = prefix + url;
        for (var i = 0, len = elems.length; i < len; i++) {
            var elem = elems[i];
            if (elem.src.indexOf(chk_str) > -1) {
                elem.remove();
            }
        }
    }
})();
(function () {
    website["randomString"] = function (len) {
        var t = "abcdefghijklmnopqrstuvwxyz0123456789";
        var r = [];
        var l = 16;
        var tLen = t.length;
        if (null != len)
            l = len;
        for (var i = 0; i < l; i++) {
            var c = t.charAt(Math.floor(Math.random() * tLen));
            r.push(c);
        }
        return r.join("");
    };
})();
(function () {
    (function () {
        website["get"] = function (reqObj) {
            var def = $.Deferred();
            if (null != reqObj["header"] && false == Array.isArray(reqObj["header"])) {
                console.error("header 參數必須使用 array 型態");
                def.reject();
                return;
            }
            var headers = {};
            (function () {
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
            (function () {
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
                    function (data) { return data; }
                ],
                headers: headers,
                params: params
            };
            axios.get(reqObj["url"], config).then(function (response) {
                var respObj = {
                    status: "done",
                    status_code: response.status,
                    data: response.data
                };
                def.resolve(respObj.data);
            }).catch(function (err) {
                def.reject(err);
            });
            return def;
        };
    })();
    (function () {
        website["post"] = function (reqObj) {
            var def = $.Deferred();
            if (null != reqObj["header"] && false == Array.isArray(reqObj["header"])) {
                console.error("header 參數必須使用 array 型態");
                def.reject();
                return;
            }
            var headers = {};
            (function () {
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
            (function () {
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
                    function (data) { return data; }
                ],
                headers: headers
            };
            axios.post(reqObj["url"], params, config).then(function (response) {
                var respObj = {
                    status: "done",
                    status_code: response.status,
                    data: response.data
                };
                def.resolve(respObj.data);
            }).catch(function (err) {
                def.reject(err);
            });
            return def;
        };
    })();
    (function () {
        website["post_json"] = function (reqObj) {
            var def = $.Deferred();
            if (null != reqObj["header"] && false == Array.isArray(reqObj["header"])) {
                console.error("header 參數必須使用 array 型態");
                def.reject();
                return;
            }
            var headers = {};
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
                    function (data) { return data; }
                ],
                headers: headers
            };
            axios.post(reqObj["url"], reqObj["text"]).then(function (response) {
                var respObj = {
                    status: "done",
                    status_code: response.status,
                    data: response.data
                };
                def.resolve(respObj.data);
            }).catch(function (err) {
                def.reject(err);
            });
            return def;
        };
    })();
    (function () {
        website["post_form_data"] = function (reqObj) {
            var def = $.Deferred();
            if (null != reqObj["header"] && false == Array.isArray(reqObj["header"])) {
                console.error("header 參數必須使用 array 型態");
                def.reject();
                return;
            }
            var headers = {};
            (function () {
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
            var formData = new FormData();
            (function () {
                if (null != reqObj["data"]) {
                    var arr = reqObj["data"];
                    for (var index in arr) {
                        var obj = arr[index];
                        formData.append(obj["key"], obj["value"]);
                    }
                }
            })();
            var config = {
                transformResponse: [function (data) { return data; }],
                headers: headers,
                onUploadProgress: function (progressEvent) {
                    var percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
                    var percentStr = String(percentCompleted);
                    var respObj = {
                        status: "upload_progress",
                        progress_value: percentStr
                    };
                    def.notify(respObj);
                }
            };
            axios.post(reqObj["url"], formData, config).then(function (response) {
                var respObj = {
                    status: "done",
                    status_code: response.status,
                    data: response.data
                };
                def.resolve(respObj.data);
            }).catch(function (err) {
                def.reject(err);
            });
            return def;
        };
    })();
})();
(function () {
    website["dialog"] = function (initObj) {
        var def = $.Deferred();
        var dialogId = website.randomString(16);
        var dialogElem = $(buildDialogHtml());
        (function () {
            dialogElem.attr("modal_dialog_ssid", dialogId);
            dialogElem.css("z-index", "10");
            dialogElem.css("background-color", "rgba(90,90,90,0.5)");
            if (null != initObj["content"] && initObj["content"].length > 0) {
                dialogElem.find("div[modal_dialog_key=wrap]").append(initObj["content"]);
            }
        })();
        $("body").append(dialogElem);
        dialogElem.css("display", "flex");
        var dialogObj = {
            dialog: dialogElem.find("div[modal_dialog_key=wrap]"),
            overlay: dialogElem,
            close: function () {
                dialogElem.remove();
            }
        };
        (function () {
            dialogElem.on("keydown", function (evt) {
                if (27 == evt.keyCode) {
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
        tmp.push("<div modal_dialog_key='wrap' style='margin: auto'></div>");
        tmp.push("</div>");
        return tmp.join('');
    }
})();
(function () {
    website["redirect"] = function (url, onCache, ran_str) {
        if (null == onCache)
            onCache = true;
        var targetURL = null;
        if (null == url) {
            targetURL = location.protocol + '//' + location.host + location.pathname;
        }
        else {
            targetURL = url;
        }
        if (onCache) {
            location.href = targetURL;
        }
        else {
            var ts = Date.now();
            var _ran_str = ts;
            if (null != ran_str)
                _ran_str = ran_str;
            location.href = targetURL + "?ei=" + _ran_str;
        }
    };
})();
var $ = window.$ || null;
(function () {
    var arr = [];
    (function () {
        arr.push("/testasync/js/jquery/jquery.min.js");
        arr.push("/testasync/js/axios/axios.min.js");
    })();
    (function () {
        var tmpPath = null;
        loadNext();
        function loadNext() {
            tmpPath = arr.shift();
            if (null != tmpPath) {
                website.script(tmpPath, function () {
                    loadNext();
                });
            }
            else {
                scriptReady();
            }
        }
    })();
    function scriptReady() {
        if (null == website.ready) {
            console.error("該頁面不具有 window.website.ready 方法，無法完成初始化呼叫");
        }
        else {
            setTimeout(function () {
                website.ready();
            }, 1);
        }
    }
})();
(function () {
    website.ready = function () {
        var page_key = $("body").attr("page_key");
        switch (page_key) {
            case "index":
                {
                    page_index();
                }
                break;
        }
    };
    function page_index() {
        (function () {
            $("#btn_select_file").on("click", function () {
                $("#upfile").click();
            });
        })();
        (function () {
            $("#label_select_file").html("[未選擇檔案]");
            $("#upfile").on("change", function () {
                var targetFile = $("#upfile")[0]["files"][0];
                if (null != targetFile) {
                    $("#label_select_file").html(targetFile.name);
                }
                else {
                    $("#label_select_file").html("[未選擇檔案]");
                }
            });
        })();
        (function () {
            $("#btn_upload_submit").on("click", function () {
                var targetFile = $("#upfile")[0]["files"][0];
                (function () {
                    var reqObj = {
                        url: "/testasync/index",
                        data: [
                            { key: "a", value: "100" },
                            { key: "b", value: "ABC" },
                            { key: "c", value: "國字測試" }
                        ]
                    };
                    if (null != targetFile) {
                        reqObj.data.push({
                            key: "myfile",
                            value: targetFile
                        });
                    }
                    website.post_form_data(reqObj)
                        .progress(function (prog) {
                        console.log(prog);
                    })
                        .done(function (respd) {
                        console.log(respd);
                    });
                })();
            });
        })();
        (function () {
            function openDialog() {
                website.dialog({
                    content: "<div style='position: relative; width: 400px;height: 400px;margin: auto 0px auto 0px;background-color: #fff;border-radius: 5px;'><span dialog_btn='close' style='position: absolute;top: 20px;right: 20px;'>&times;</span>TEST</div>"
                }).done(function (dialogObj) {
                    console.log(dialogObj);
                    dialogObj.overlay.css("background-color", "rgba(190,190,190,0.5)");
                    dialogObj.dialog.find("span[dialog_btn=close]").on("click", function () {
                        dialogObj.close();
                    });
                });
            }
            $("button[ui_key=btn_open_dialog]").on("click", function () {
                openDialog();
            });
        })();
        (function () {
            $("button[ui_key=btn_test_get]").on("click", function () {
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
                }).done(function (respd) {
                    console.log(respd);
                });
            });
        })();
        (function () {
            $("button[ui_key=btn_test_post]").on("click", function () {
                website.post({
                    url: "/testasync/index",
                    data: [
                        { key: "a", value: "100" },
                        { key: "b", value: "ABC" },
                        { key: "c", value: "國字測試" },
                        { key: "d", value: "/// //aaa// a//cc[]///" }
                    ]
                }).done(function (respd) {
                    console.log(respd);
                });
            });
        })();
        (function () {
            function asyncTest() {
                setTimeout(function () {
                    var reqObj = {
                        url: "/testasync/index",
                        data: [
                            { key: "A", value: "100" },
                            { key: "B", value: "200" },
                            { key: "C", value: "300" }
                        ]
                    };
                    website.get(reqObj).done(function (respd) {
                        var obj = JSON.parse(respd);
                        console.log(obj);
                    });
                }, 1000);
            }
            asyncTest();
        })();
    }
})();
