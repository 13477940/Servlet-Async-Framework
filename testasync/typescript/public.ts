// namespace define
var debug_mode: boolean = true; // for try-catch, scope: ajax
var website: any = window.website || {};
var $: any = window.$ || null; // jquery
var axios: any = window.axios || null;
var CryptoJS: any = window.CryptoJS || null;
var moment: any = window.moment || null;

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
            // loop check page ready status.
            exec_load_fn();
            // 載入腳本實作
            function exec_load_fn() {
                // 如果沒有在這個時候載入會導致 log cat 無法接收所有此時載入的腳本錯誤 exception
                if("complete" == document.readyState) {
                    var framework_label = "my_script_loader";
                    var container_div: any = document.querySelector("div[_framework_key="+framework_label+"]");
                    (function(){
                        if(null == container_div) {
                            container_div = document.createElement('div');
                            container_div.setAttribute("_framework_key", framework_label); // set elem attr
                            document.body.appendChild(container_div);
                        }
                    })();
                    var scriptTag = document.createElement('script');
                    scriptTag.src = url;
                    scriptTag.onload = readyFn;
                    scriptTag.onerror = loadErrorFn;
                    // document.body.appendChild(scriptTag);
                    (function(){
                        container_div.append(scriptTag);
                    })();
                } else {
                    setTimeout(function(){
                        exec_load_fn();
                    }, 100);
                }
            }
            // 當腳本讀取發生錯誤時
            function loadErrorFn(oError: any) {
                console.error(oError);
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
                if(debug_mode) console.error(err);
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
            }).catch(function(err: any) {
                if(debug_mode) console.error(err);
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
            axios.post(reqObj["url"], reqObj["text"]).then(function(response: any) {
                var respObj = {
                    status: "done",
                    status_code: response.status,
                    data: response.data
                };
                def.resolve(respObj.data);
            }).catch(function(err: any) {
                if(debug_mode) console.error(err);
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
                if(debug_mode) console.error(err);
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
        var dialogId = "_dialog_"+website.randomString(16);
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
            var enable_esc_key = "true";
            if(null != initObj["escape_key"]) {
                enable_esc_key = initObj["escape_key"];
            }
            if("true" == enable_esc_key) {
                dialogElem.on("keydown", function(evt: any){
                    if(27 == evt.keyCode) {
                        dialogObj.close();
                    }
                });
            }
        })();
        // focus to this ( fix for use tab key )
        (function(){
            dialogElem.focus();
        })();
        def.resolve(dialogObj);
        return def;
    };
    function buildDialogHtml() {
        var overlay_elem = $("<div modal_dialog_key='overlay' tabindex='0'></div>");
        (function(){
            overlay_elem.css("display", "none");
            overlay_elem.css("align-items", "center");
            overlay_elem.css("justify-content", "center");
            overlay_elem.css("position", "fixed");
            overlay_elem.css("top", "0px");
            overlay_elem.css("left", "0px");
            overlay_elem.css("width", "100vw");
            overlay_elem.css("height", "100vh");
            overlay_elem.css("overflow", "auto");
            overlay_elem.css("backdrop-filter", "blur(1px)"); // 毛玻璃效果
        })();
        overlay_elem.html("<div modal_dialog_key='wrap' style='position: absolute;'></div>");
        return overlay_elem.prop("outerHTML");
    }
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

// base64 url safe
(function(){
    website["base64_url_encode"] = function( content: any ) {
        if(null == content) {
            console.error("b64 encode content is null!");
            return null;
        }
        var byteArr = CryptoJS.enc.Utf8.parse( content );
        var res_str = url_safe_withoutPadding( CryptoJS.enc.Base64.stringify(byteArr) );
        return res_str;
    };
    website["base64_url_decode"] = function( content: any ) {
        if(null == content) {
            console.error("b64 decode content is null!");
            return null;
        }
        var byteArr = CryptoJS.enc.Base64.parse( decode_url_safe( content ) );
        var res_str = byteArr.toString(CryptoJS.enc.Utf8);
        return res_str;
    };
    // https://jsfiddle.net/magikMaker/7bjaT/
    function url_safe_withoutPadding(str: any) {
        return str.replace(/\+/g, '-').replace(/\//g, '_').replace(/\=+$/, '');
    }
    // https://jsfiddle.net/magikMaker/7bjaT/
    function decode_url_safe(str: any) {
        return str.replace(/-/g, '+').replace(/_/g, '/');
    }
})();

// number format currency style
// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/NumberFormat
(function(){
    website["currency_number"] = function( num_val: number ) {
        // for taiwan used
        // return new Intl.NumberFormat('zh-TW', { style: 'currency', currency: 'TWD' }).format( num_val );
        return new Intl.NumberFormat('zh-TW', { style: 'decimal', currency: 'TWD' }).format( num_val );
    };
})();

// print command
// https://stackoverflow.com/questions/28343748/google-chrome-print-preview-does-not-load-the-page-the-first-time
(function(){
    website["print"] = function( elem: any ){
        // default print content preview
        // window.open('tab_url', 'tab_name', 'width=800,height=600');
        var w_width = $(window).width() * 0.9;
        var w_height = $(window).height() * 0.9;
        (function(){
            // limit preview size
            if(600 > w_width) w_width = 600;
            if(600 > w_height) w_height = 600;
        })();
        var mywindow: any = window.open('','','left=0,top=0,width='+w_width+',height='+w_height+',toolbar=0,scrollbars=0,status=0,addressbar=0');
        var is_chrome = Boolean(mywindow.chrome);
        mywindow.document.write( elem.prop("outerHTML") );
        mywindow.document.close(); // necessary for IE >= 10 and necessary before onload for chrome
        if(is_chrome) {
            var b_need_load = false;
            // 確認是否需要使用到 onload 狀態（如頁面沒有這個需求會導致無法觸發 onload）
            if ( elem.prop("outerHTML").indexOf("<img") == -1 ) b_need_load = true;
            if( b_need_load ) {
                mywindow.focus(); // necessary for IE >= 10
                mywindow.print();
                mywindow.close();
            } else {
                mywindow.onload = function() { // wait until all resources loaded
                    mywindow.focus(); // necessary for IE >= 10
                    mywindow.print(); // change window to mywindow
                    mywindow.close(); // change window to mywindow
                };
            }
        } else {
            mywindow.document.close(); // necessary for IE >= 10
            mywindow.focus(); // necessary for IE >= 10
            mywindow.print();
            mywindow.close();
        }
    };
})();

// get os browser scroll width size
(function(){
    website["scroll_width"] = function() {
        var scr = null;
        var inn = null;
        var wNoScroll = 0;
        var wScroll = 0;
        // Outer scrolling div
        scr = document.createElement('div');
        var div_id = "_scroll_test" + website.randomString(16);
        scr.id = div_id;
        scr.style.position = 'absolute';
        scr.style.top = '-1000px';
        scr.style.left = '-1000px';
        scr.style.width = '100px';
        scr.style.height = '50px';
        // scr.padding='0px';
        // scr.margin='0px';
        // Start with no scrollbar
        scr.style.overflow = 'hidden';
        // Inner content div
        inn = document.createElement('div');
        inn.style.width = '100%';
        inn.style.height = '200px';
        // Put the inner div in the scrolling div
        scr.appendChild(inn);
        // Append the scrolling div to the doc
        document.getElementsByTagName('html')[0].appendChild(scr);
        // Width of the inner div sans scrollbar
        wNoScroll = inn.offsetWidth;
        // Add the scrollbar
        scr.style.overflow = 'auto';
        // Width of the inner div width scrollbar
        wScroll = inn.offsetWidth;
        // Remove the scrolling div from the doc
        var elem = document.getElementById(div_id);
        if(null != elem) elem.remove();
        // Pixel width of the scroller
        return (wNoScroll - wScroll);
    };
})();

(function(){
    // ( 輸入值, 處理四捨五入到小數點第幾位 )
    website["proc_float"] = function(number: any, fractionDigits: any) {
        var def = fractionDigits || 2;
        return Math.round(number* Math.pow(10, def))/ Math.pow(10, def);
    };
})();
