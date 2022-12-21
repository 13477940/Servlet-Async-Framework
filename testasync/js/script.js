"use strict";
var debug_mode = true;
var website = window.website || {};
var $ = window.$ || null;
var axios = window.axios || null;
(function () {
    website["script"] = function (script_url, ready_fn) {
        (function () {
            let timer = null;
            exec_load_fn();
            function exec_load_fn() {
                if (null != timer)
                    clearTimeout(timer);
                timer = setTimeout(function () {
                    if ("complete" == document.readyState) {
                        load_script_fn(script_url, ready_fn);
                    }
                    else {
                        exec_load_fn();
                    }
                }, 60);
            }
        })();
    };
    function load_script_fn(script_url, ready_fn) {
        if (null == ready_fn)
            ready_fn = function () { };
        const framework_label = "my_script_loader";
        let container_div = document.querySelector("div[_framework_key=" + framework_label + "]");
        (function () {
            if (null == container_div) {
                container_div = document.createElement('div');
                container_div.setAttribute("_framework_key", framework_label);
                document.body.appendChild(container_div);
            }
        })();
        let catch_count = 0;
        let target_count = 0;
        (function () {
            if (Array.isArray(script_url)) {
                target_count = script_url.length;
                for (let url_index in script_url) {
                    let url = script_url[url_index];
                    rm_rep_script(url);
                    append_script_tag(url, container_div, catch_script_done, catch_script_fail);
                }
            }
            else {
                target_count = 1;
                rm_rep_script(script_url);
                append_script_tag(script_url, container_div, catch_script_done, catch_script_fail);
            }
        })();
        function catch_script_done(evt) {
            catch_count++;
            if (target_count == catch_count)
                ready_fn();
        }
        function catch_script_fail(evt) {
            catch_count++;
            if (target_count == catch_count)
                ready_fn();
            console.error(evt);
        }
    }
    function append_script_tag(url, container_div, ready_fn, error_fn) {
        let scriptTag = document.createElement('script');
        scriptTag.src = url;
        if (null != ready_fn)
            scriptTag.onload = ready_fn;
        if (null != error_fn)
            scriptTag.onerror = error_fn;
        container_div.append(scriptTag);
    }
    let prefix = [];
    (function () {
        prefix.push(window.location.protocol);
        prefix.push("//");
        prefix.push(window.location.hostname);
    })();
    function rm_rep_script(url) {
        let elems = document.querySelectorAll("script");
        let chk_str = prefix.join("") + url;
        for (let i = 0, len = elems.length; i < len; i++) {
            let elem = elems[i];
            if (elem.src.includes(chk_str)) {
                elem.remove();
            }
        }
    }
})();
(function () {
    website["randomString"] = function (len) {
        const t = "abcdefghijklmnopqrstuvwxyz0123456789";
        let r = [];
        let l = 16;
        let tLen = t.length;
        if (null != len)
            l = len;
        for (let i = 0; i < l; i++) {
            const c = t.charAt(Math.floor(Math.random() * tLen));
            r.push(c);
        }
        return r.join("");
    };
})();
(function () {
    (function () {
        website["get"] = function (reqObj) {
            const def = $.Deferred();
            if (null != reqObj["header"] && false == Array.isArray(reqObj["header"])) {
                console.error("header 參數必須使用 array 型態");
                def.reject();
                return;
            }
            const headers = {};
            (function () {
                let arr = reqObj["header"];
                for (let index in arr) {
                    let obj = arr[index];
                    let key = obj["key"];
                    let value = obj["value"];
                    headers[key] = value;
                }
            })();
            if (null != reqObj["data"] && false == Array.isArray(reqObj["data"])) {
                console.error("data 參數必須使用 array 型態");
                def.reject();
                return;
            }
            const params = new URLSearchParams();
            (function () {
                let arr = reqObj["data"];
                for (let index in arr) {
                    let obj = arr[index];
                    let key = obj["key"];
                    let value = obj["value"];
                    params.append(key, value);
                }
            })();
            const config = {
                transformResponse: [
                    function (data) { return data; }
                ],
                headers: headers,
                params: params
            };
            axios.get(reqObj["url"], config).then(function (response) {
                let respObj = {
                    status: "done",
                    status_code: response.status,
                    data: response.data
                };
                def.resolve(respObj.data);
            }).catch(function (err) {
                if (debug_mode)
                    console.error(err);
                def.reject(err);
            });
            return def;
        };
    })();
    (function () {
        website["post"] = function (reqObj) {
            const def = $.Deferred();
            if (null != reqObj["header"] && false == Array.isArray(reqObj["header"])) {
                console.error("header 參數必須使用 array 型態");
                def.reject();
                return;
            }
            const headers = {};
            (function () {
                let arr = reqObj["header"];
                for (let index in arr) {
                    let obj = arr[index];
                    let key = obj["key"];
                    let value = obj["value"];
                    headers[key] = value;
                }
            })();
            if (null != reqObj["data"] && false == Array.isArray(reqObj["data"])) {
                console.error("data 參數必須使用 array 型態");
                def.reject();
                return;
            }
            const params = new URLSearchParams();
            (function () {
                let arr = reqObj["data"];
                for (let index in arr) {
                    let obj = arr[index];
                    let key = obj["key"];
                    let value = obj["value"];
                    params.append(key, value);
                }
            })();
            const config = {
                transformResponse: [
                    function (data) { return data; }
                ],
                headers: headers
            };
            axios.post(reqObj["url"], params, config).then(function (response) {
                let respObj = {
                    status: "done",
                    status_code: response.status,
                    data: response.data
                };
                def.resolve(respObj.data);
            }).catch(function (err) {
                if (debug_mode)
                    console.error(err);
                def.reject(err);
            });
            return def;
        };
    })();
    (function () {
        website["post_json"] = function (reqObj) {
            const def = $.Deferred();
            if (null != reqObj["header"] && false == Array.isArray(reqObj["header"])) {
                console.error("header 參數必須使用 array 型態");
                def.reject();
                return;
            }
            const headers = {};
            (function () {
                let arr = reqObj["header"];
                for (let index in arr) {
                    let obj = arr[index];
                    let key = obj["key"];
                    let value = obj["value"];
                    headers[key] = value;
                }
            })();
            axios.post(reqObj["url"], reqObj["text"]).then(function (response) {
                let respObj = {
                    status: "done",
                    status_code: response.status,
                    data: response.data
                };
                def.resolve(respObj.data);
            }).catch(function (err) {
                if (debug_mode)
                    console.error(err);
                def.reject(err);
            });
            return def;
        };
    })();
    (function () {
        website["post_form_data"] = function (reqObj) {
            const def = $.Deferred();
            if (null != reqObj["header"] && false == Array.isArray(reqObj["header"])) {
                console.error("header 參數必須使用 array 型態");
                def.reject();
                return;
            }
            const headers = {};
            (function () {
                let arr = reqObj["header"];
                for (let index in arr) {
                    let obj = arr[index];
                    let key = obj["key"];
                    let value = obj["value"];
                    headers[key] = value;
                }
            })();
            if (null != reqObj["data"] && false == Array.isArray(reqObj["data"])) {
                console.error("data 參數必須使用 array 型態");
                def.reject();
                return;
            }
            const formData = new FormData();
            (function () {
                if (null != reqObj["data"]) {
                    let arr = reqObj["data"];
                    for (let index in arr) {
                        let obj = arr[index];
                        formData.append(obj["key"], obj["value"]);
                    }
                }
            })();
            const config = {
                transformResponse: [function (data) { return data; }],
                headers: headers,
                onUploadProgress: function (progressEvent) {
                    let percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
                    let percentStr = String(percentCompleted);
                    let respObj = {
                        status: "upload_progress",
                        progress_value: percentStr
                    };
                    def.notify(respObj);
                }
            };
            axios.post(reqObj["url"], formData, config).then(function (response) {
                let respObj = {
                    status: "done",
                    status_code: response.status,
                    data: response.data
                };
                def.resolve(respObj.data);
            }).catch(function (err) {
                if (debug_mode)
                    console.error(err);
                def.reject(err);
            });
            return def;
        };
    })();
})();
(function () {
    website["dialog"] = function (initObj) {
        const def = $.Deferred();
        const dialogId = "_dialog_" + website.randomString(16);
        const dialogElem = $(buildDialogHtml());
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
        const dialogObj = {
            dialog: dialogElem.find("div[modal_dialog_key=wrap]"),
            overlay: dialogElem,
            close: function () {
                dialogElem.remove();
            }
        };
        (function () {
            let enable_esc_key = "true";
            if (null != initObj["escape_key"]) {
                enable_esc_key = initObj["escape_key"];
            }
            if ("true" == enable_esc_key) {
                dialogElem.on("keydown", function (evt) {
                    if (27 == evt.keyCode) {
                        dialogObj.close();
                    }
                });
            }
        })();
        (function () {
            dialogElem.focus();
        })();
        def.resolve(dialogObj);
        return def;
    };
    function buildDialogHtml() {
        const overlay_elem = $("<div modal_dialog_key='overlay' tabindex='0'></div>");
        (function () {
            overlay_elem.css("display", "none");
            overlay_elem.css("align-items", "stretch");
            overlay_elem.css("justify-content", "stretch");
            overlay_elem.css("position", "fixed");
            overlay_elem.css("top", "0px");
            overlay_elem.css("left", "0px");
            overlay_elem.css("width", "100vw");
            overlay_elem.css("height", "100vh");
            overlay_elem.css("overflow", "auto");
            overlay_elem.css("backdrop-filter", "blur(1px)");
        })();
        (function () {
            const wrap_html = [];
            wrap_html.push("<div style='flex: 1;'></div>");
            wrap_html.push("<div style='flex-shrink: 0;display: flex;align-items: stretch;justify-content: stretch;flex-direction: column;'>");
            wrap_html.push("<div style='flex: 1;'></div>");
            wrap_html.push("<div modal_dialog_key='wrap' style='flex-shrink: 0;'></div>");
            wrap_html.push("<div style='flex: 1;'></div>");
            wrap_html.push("</div>");
            wrap_html.push("<div style='flex: 1;'></div>");
            overlay_elem.html(wrap_html.join(""));
        })();
        return overlay_elem.prop("outerHTML");
    }
})();
(function () {
    website["redirect"] = function (url, onCache) {
        if (null == onCache)
            onCache = true;
        let targetURL = null;
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
            const ts = Date.now();
            location.href = targetURL + "?ei=" + ts;
        }
    };
})();
(function () {
    website["base64_encode"] = function (content) {
        if (null == content) {
            console.error("b64 encode content is null!");
            return null;
        }
        const text_encoder = new TextEncoder();
        const aMyUTF8Input = text_encoder.encode(content);
        return base64EncArr(aMyUTF8Input);
    };
    website["base64_url_encode"] = function (content) {
        return url_safe_withoutPadding(website["base64_encode"](content));
    };
    website["base64_decode"] = function (content) {
        if (null == content) {
            console.error("b64 decode content is null!");
            return null;
        }
        const text_decoder = new TextDecoder();
        const aMyUTF8Output = base64DecToArr(content, 1);
        return text_decoder.decode(aMyUTF8Output);
    };
    website["base64_url_decode"] = function (content) {
        return website["base64_decode"](content);
    };
    website["base64_enc_byte"] = function (src_byte) {
        const tmp_arr = new Uint8Array(src_byte);
        return base64EncArr(tmp_arr);
    };
    website["base64_dec_byte"] = function (src_byte) {
        return base64DecToArr(src_byte, 1);
    };
    website["base64_url_enc_byte"] = function (src_byte) {
        return url_safe_withoutPadding(website["base64_enc_byte"](src_byte));
    };
    website["base64_url_dec_byte"] = function (src_byte) {
        return website["base64_dec_byte"](src_byte);
    };
    function url_safe_withoutPadding(str) {
        return str.replace(/\+/g, '-').replace(/\//g, '_').replace(/\=+$/, '');
    }
    function decode_url_safe(str) {
        return str.replace(/-/g, '+').replace(/_/g, '/');
    }
    function uint6ToB64(nUint6) {
        return nUint6 < 26
            ? nUint6 + 65
            : nUint6 < 52
                ? nUint6 + 71
                : nUint6 < 62
                    ? nUint6 - 4
                    : nUint6 === 62
                        ? 43
                        : nUint6 === 63
                            ? 47
                            : 65;
    }
    function base64EncArr(aBytes) {
        let nMod3 = 2;
        let sB64Enc = "";
        const nLen = aBytes.length;
        let nUint24 = 0;
        for (let nIdx = 0; nIdx < nLen; nIdx++) {
            nMod3 = nIdx % 3;
            if (nIdx > 0 && ((nIdx * 4) / 3) % 76 === 0) {
            }
            nUint24 |= aBytes[nIdx] << ((16 >>> nMod3) & 24);
            if (nMod3 === 2 || aBytes.length - nIdx === 1) {
                sB64Enc += String.fromCodePoint(uint6ToB64((nUint24 >>> 18) & 63), uint6ToB64((nUint24 >>> 12) & 63), uint6ToB64((nUint24 >>> 6) & 63), uint6ToB64(nUint24 & 63));
                nUint24 = 0;
            }
        }
        return (sB64Enc.substring(0, sB64Enc.length - 2 + nMod3) + (nMod3 === 2 ? "" : nMod3 === 1 ? "=" : "=="));
    }
    function b64ToUint6(nChr) {
        return nChr > 64 && nChr < 91
            ? nChr - 65
            : nChr > 96 && nChr < 123
                ? nChr - 71
                : nChr > 47 && nChr < 58
                    ? nChr + 4
                    : nChr === 43
                        ? 62
                        : nChr === 47
                            ? 63
                            : 0;
    }
    function base64DecToArr(sBase64, nBlocksSize) {
        const sB64Enc = sBase64.replace(/[^A-Za-z0-9+/]/g, "");
        const nInLen = sB64Enc.length;
        const nOutLen = nBlocksSize
            ? Math.ceil(((nInLen * 3 + 1) >> 2) / nBlocksSize) * nBlocksSize
            : (nInLen * 3 + 1) >> 2;
        const taBytes = new Uint8Array(nOutLen);
        let nMod3;
        let nMod4;
        let nUint24 = 0;
        let nOutIdx = 0;
        for (let nInIdx = 0; nInIdx < nInLen; nInIdx++) {
            nMod4 = nInIdx & 3;
            nUint24 |= b64ToUint6(sB64Enc.charCodeAt(nInIdx)) << (6 * (3 - nMod4));
            if (nMod4 === 3 || nInLen - nInIdx === 1) {
                nMod3 = 0;
                while (nMod3 < 3 && nOutIdx < nOutLen) {
                    taBytes[nOutIdx] = (nUint24 >>> ((16 >>> nMod3) & 24)) & 255;
                    nMod3++;
                    nOutIdx++;
                }
                nUint24 = 0;
            }
        }
        return taBytes;
    }
})();
(function () {
    website["currency_number"] = function (num_val) {
        return new Intl.NumberFormat('zh-TW', { style: 'decimal', currency: 'TWD' }).format(num_val);
    };
})();
(function () {
    website["print"] = function (elem, width, height) {
        var w_width = $(window).width() * 0.9;
        if (null != width)
            w_width = width;
        var w_height = $(window).height() * 0.9;
        if (null != w_height)
            w_height = height;
        (function () {
            if (600 > w_width)
                w_width = 600;
            if (600 > w_height)
                w_height = 600;
        })();
        var mywindow = window.open('', '', 'left=0,top=0,width=' + w_width + ',height=' + w_height + ',toolbar=0,scrollbars=0,status=0,addressbar=0');
        var is_chrome = Boolean(mywindow.chrome);
        mywindow.document.write(elem.prop("outerHTML"));
        mywindow.document.close();
        if (is_chrome) {
            var b_need_load = false;
            if (elem.prop("outerHTML").includes("<img"))
                b_need_load = true;
            if (b_need_load) {
                mywindow.onload = function () {
                    mywindow.focus();
                    mywindow.print();
                    mywindow.close();
                };
            }
            else {
                mywindow.focus();
                mywindow.print();
                mywindow.close();
            }
        }
        else {
            mywindow.document.close();
            mywindow.focus();
            mywindow.print();
            mywindow.close();
        }
    };
})();
(function () {
    website["scroll_width"] = function () {
        var scr = null;
        var inn = null;
        var wNoScroll = 0;
        var wScroll = 0;
        scr = document.createElement('div');
        var div_id = "_scroll_test_" + website.randomString(16);
        scr.id = div_id;
        scr.style.position = 'absolute';
        scr.style.top = '-1000px';
        scr.style.left = '-1000px';
        scr.style.width = '100px';
        scr.style.height = '50px';
        scr.style.overflow = 'hidden';
        inn = document.createElement('div');
        inn.style.width = '100%';
        inn.style.height = '200px';
        scr.appendChild(inn);
        document.getElementsByTagName('html')[0].appendChild(scr);
        wNoScroll = inn.offsetWidth;
        scr.style.overflow = 'auto';
        wScroll = inn.offsetWidth;
        var elem = document.getElementById(div_id);
        if (null != elem)
            elem.remove();
        return (wNoScroll - wScroll);
    };
})();
(function () {
    website["float"] = function (number, fractionDigits) {
        var def = fractionDigits || 2;
        return Math.round(number * Math.pow(10, def)) / Math.pow(10, def);
    };
})();
(function () {
    website["replace_all"] = function (str, scan_str, fix_str) {
        return str.split(scan_str).join(fix_str);
    };
})();
(function () {
    var arr = [];
    (function () {
        arr.push("/testasync/js/jquery/jquery.min.js");
        arr.push("/testasync/js/axios/axios.min.js");
        arr.push("/testasync/js/index.js");
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
    }
})();
