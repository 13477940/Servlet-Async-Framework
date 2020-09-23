/*
https://caniuse.com/#feat=webworkers
https://github.com/axios/axios

#2020-04-28 修正錯誤中斷點的回傳內容
#2020-04-28 增加自定義 request header 的功能
*/

"use strict"
self.importScripts("/testasync/js/axios/axios.min.js");
self.addEventListener("message", workerReady, false);
var workerFn = {};

function workerReady(e) {
    var reqObj = e.data.reqObj;
    switch(reqObj["act"]) {
        case "get": { workerFn["get"](reqObj); } break;
        case "post": { workerFn["post"](reqObj); } break;
        case "post_form_data": { workerFn["post_form_data"](reqObj); } break;
    }
}

(function(){
    // get with application/x-www-form-urlencoded;charset=UTF-8
    workerFn["get"] = function(reqObj) {
        if(null != reqObj["data"] && false == Array.isArray(reqObj["data"])) {
            var respObj = {
                status: "error",
                msg_zht: "data 參數必須使用 array 型態"
            };
            self.postMessage(respObj);
            return;
        }
        var params = new URLSearchParams();
        (function(){
            var arr = reqObj["data"];
            for(var index in arr) {
                var obj = arr[index];
                var key = obj["key"];
                var value = obj["value"];
                params.append(key, value);
            }
        })();
        if(null != reqObj["header"] && false == Array.isArray(reqObj["header"])) {
            var respObj = {
                status: "error",
                msg_zht: "header 參數必須使用 array 型態"
            };
            self.postMessage(respObj);
            return;
        }
        var headers = {};
        (function(){
            var arr = reqObj["header"];
            for(var index in arr) {
                var obj = arr[index];
                var key = obj["key"];
                var value = obj["value"];
                headers[key] = value;
            }
        })();
        var config = {
            transformResponse: [(data) => { return data; }], // 修正 response 回傳內容格式
            headers: headers,
            params: params
        };
        axios.get(reqObj["url"], config).then(function (response) {
            var respObj = {
                status: "done",
                status_code: response.status,
                data: response.data
            };
            self.postMessage(respObj);
        }).catch(function (err) {
            var respObj = {
                status: "error",
                error: err
            };
            self.postMessage(respObj);
        });
    };
})();

(function(){
    // post form-data with application/x-www-form-urlencoded;charset=UTF-8
    // https://medium.com/@jacobhsu/api-%E5%9B%9B%E7%A8%AE%E5%B8%B8%E8%A6%8B%E7%9A%84-post-%E6%8F%90%E4%BA%A4%E6%95%B8%E6%93%9A%E6%96%B9%E5%BC%8F-5d93ccea919d
    workerFn["post"] = function(reqObj) {
        if(null != reqObj["data"] && false == Array.isArray(reqObj["data"])) {
            var respObj = {
                status: "error",
                msg_zht: "data 參數必須使用 array 型態"
            };
            self.postMessage(respObj);
            return;
        }
        // https://github.com/axios/axios#using-applicationx-www-form-urlencoded-format
        var params = new URLSearchParams();
        (function(){
            var arr = reqObj["data"];
            for(var index in arr) {
                var obj = arr[index];
                var key = obj["key"];
                var value = obj["value"];
                params.append(key, value);
            }
        })();
        if(null != reqObj["header"] && false == Array.isArray(reqObj["header"])) {
            var respObj = {
                status: "error",
                msg_zht: "header 參數必須使用 array 型態"
            };
            self.postMessage(respObj);
            return;
        }
        var headers = {};
        (function(){
            var arr = reqObj["header"];
            for(var index in arr) {
                var obj = arr[index];
                var key = obj["key"];
                var value = obj["value"];
                headers[key] = value;
            }
        })();
        var config = {
            transformResponse: [(data) => { return data; }], // 修正 response 回傳內容格式
            headers: headers
        };
        axios.post(reqObj["url"], params, config).then(function(response) {
            var respObj = {
                status: "done",
                status_code: response.status,
                data: response.data
            };
            self.postMessage(respObj);
        }).catch(function (err) {
            var respObj = {
                status: "error",
                error: err
            };
            self.postMessage(respObj);
        });
    };
})();

(function(){
    // post form-data with multipart/form-data
    workerFn["post_form_data"] = function(reqObj) {
        if(null != reqObj["data"] && false == Array.isArray(reqObj["data"])) {
            var respObj = {
                status: "error",
                msg_zht: "data 參數必須使用 array 型態"
            };
            self.postMessage(respObj);
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
        if(null != reqObj["header"] && false == Array.isArray(reqObj["header"])) {
            var respObj = {
                status: "error",
                msg_zht: "header 參數必須使用 array 型態"
            };
            self.postMessage(respObj);
            return;
        }
        var headers = {};
        (function(){
            var arr = reqObj["header"];
            for(var index in arr) {
                var obj = arr[index];
                var key = obj["key"];
                var value = obj["value"];
                headers[key] = value;
            }
        })();
        var config = {
            transformResponse: [(data) => { return data; }], // 修正 response 回傳內容格式
            headers: headers,
            // maxContentLength: 20000000,
            onUploadProgress: function(progressEvent) {
                var percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
                var percentStr = String(percentCompleted); // percent number
                var respObj = {
                    status: "upload_progress",
                    progress_value: percentStr
                };
                self.postMessage(respObj);
            }
        };
        // form-data 格式請使用 post method
        axios.post(reqObj["url"], formData, config).then(function (response) {
            var respObj = {
                status: "done",
                status_code: response.status,
                data: response.data
            };
            self.postMessage(respObj);
        }).catch(function (err) {
            var respObj = {
                status: "error",
                error: err
            };
            self.postMessage(respObj);
        });
    };
})();
