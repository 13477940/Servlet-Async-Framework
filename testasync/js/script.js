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
                if (0 == target_count) {
                    ready_fn();
                    return;
                }
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
    website["random_string"] = function (len) {
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
    website["cookie"] = {
        getItem: function (sKey) {
            return decodeURIComponent(document.cookie.replace(new RegExp("(?:(?:^|.*;)\\s*" + encodeURIComponent(sKey).replace(/[-.+*]/g, "\\$&") + "\\s*\\=\\s*([^;]*).*$)|^.*$"), "$1")) || null;
        },
        setItem: function (sKey, sValue, vEnd, sPath, sDomain, bSecure) {
            if (!sKey || /^(?:expires|max\-age|path|domain|secure)$/i.test(sKey)) {
                return false;
            }
            let sExpires = "";
            if (vEnd) {
                switch (vEnd.constructor) {
                    case Number:
                        {
                            sExpires = vEnd === Infinity ? "; expires=Fri, 31 Dec 9999 23:59:59 GMT" : "; max-age=" + vEnd;
                        }
                        break;
                    case String:
                        {
                            sExpires = "; expires=" + vEnd;
                        }
                        break;
                    case Date:
                        {
                            sExpires = "; expires=" + vEnd.toUTCString();
                        }
                        break;
                }
            }
            document.cookie = encodeURIComponent(sKey) + "=" + encodeURIComponent(sValue) + sExpires + (sDomain ? "; domain=" + sDomain : "") + (sPath ? "; path=" + sPath : "") + (bSecure ? "; secure" : "");
            return true;
        },
        removeItem: function (sKey, sPath, sDomain) {
            if (!sKey || !this.hasItem(sKey)) {
                return false;
            }
            document.cookie = encodeURIComponent(sKey) + "=; expires=Thu, 01 Jan 1970 00:00:00 GMT" + (sDomain ? "; domain=" + sDomain : "") + (sPath ? "; path=" + sPath : "");
            return true;
        },
        hasItem: function (sKey) {
            return (new RegExp("(?:^|;\\s*)" + encodeURIComponent(sKey).replace(/[-.+*]/g, "\\$&") + "\\s*\\=")).test(document.cookie);
        },
        keys: function () {
            const aKeys = document.cookie.replace(/((?:^|\s*;)[^\=]+)(?=;|$)|^\s*|\s*(?:\=[^;]*)?(?:\1|$)/g, "").split(/\s*(?:\=[^;]*)?;\s*/);
            for (let nIdx = 0; nIdx < aKeys.length; nIdx++) {
                aKeys[nIdx] = decodeURIComponent(aKeys[nIdx]);
            }
            return aKeys;
        },
        clear: function () {
            const key_arr = website["cookie"].keys();
            for (let i = 0, len = key_arr.length; i < len; i++) {
                const key = key_arr[i];
                website["cookie"].removeItem(key);
            }
        }
    };
})();
(function () {
    website["websocket"] = function (config_obj) {
        let protocol = get_websocket_protocol();
        if (null != config_obj["protocol"])
            protocol = config_obj["protocol"];
        const ws_uri = protocol + config_obj["path"];
        const web_socket = new WebSocket(ws_uri);
        web_socket.onopen = config_obj["onopen"];
        web_socket.onmessage = config_obj["onmessage"];
        web_socket.onerror = config_obj["onerror"];
        web_socket.onclose = config_obj["onclose"];
        return web_socket;
    };
    function get_websocket_protocol() {
        return location.protocol.includes('https') ? 'wss://' : 'ws://';
    }
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
                (function () {
                    headers["content-type"] = "application/x-www-form-urlencoded;charset=utf-8";
                })();
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
                (function () {
                    headers["content-type"] = "application/json;charset=utf-8";
                })();
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
                (function () {
                    headers["content-type"] = "multipart/form-data";
                })();
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
        const dialogId = "_dialog_" + website.random_string(16);
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
            const fix_window_width = window.innerWidth;
            overlay_elem.css("width", fix_window_width);
            const fix_window_height = window.innerHeight;
            overlay_elem.css("height", fix_window_height);
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
        return website["base64_decode"](decode_url_safe(content));
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
        return website["base64_dec_byte"](decode_url_safe(src_byte));
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
    website["print"] = function (elem, width, height) {
        const window_elem = $(window);
        let w_width = parseInt(window_elem.width() * 0.9 + "", 10);
        if (null != width)
            w_width = width;
        let w_height = parseInt(window_elem.height() * 0.9 + "", 10);
        if (null != height)
            w_height = height;
        setTimeout(function () {
            open_print_window(elem, w_width, w_height);
        }, 1);
    };
    function open_print_window(elem, width, height) {
        const mywindow = window.open('', '', 'left=0,top=0,width=' + width + ',height=' + height + ',toolbar=0,scrollbars=0,status=0,addressbar=0');
        const is_chrome = Boolean(mywindow.chrome);
        mywindow.document.write(elem.prop("outerHTML"));
        mywindow.document.close();
        if (is_chrome) {
            let b_need_load = false;
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
    }
})();
(function () {
    website["scroll_width"] = function () {
        let scr = null;
        let inn = null;
        let wNoScroll = 0;
        let wScroll = 0;
        const div_id = "_scroll_test_" + website.random_string(16);
        (function () {
            scr = document.createElement("div");
            scr.id = div_id;
            scr.style.position = "absolute";
            scr.style.top = "-1000px";
            scr.style.left = "-1000px";
            scr.style.width = "100px";
            scr.style.height = "100px";
            scr.style.overflow = "hidden";
        })();
        (function () {
            inn = document.createElement("div");
            inn.style.width = "100%";
            inn.style.height = "200px";
        })();
        (function () {
            scr.appendChild(inn);
            document.getElementsByTagName("html")[0].appendChild(scr);
            wNoScroll = inn.offsetWidth;
            scr.style.overflow = "auto";
            wScroll = inn.offsetWidth;
        })();
        const elem = document.getElementById(div_id);
        if (null != elem)
            elem.remove();
        return (wNoScroll - wScroll);
    };
})();
(function () {
    website["number_format"] = function (num_val, style) {
        const locales = "zh-tw";
        const options = {
            style: "currency",
            currency: "TWD",
        };
        return new Intl.NumberFormat(locales, options).format(num_val);
    };
    website["number_currency"] = function (num_val, min_digits) {
        let _min_digits = 2;
        if (null != min_digits)
            _min_digits = min_digits;
        const locales = "zh-tw";
        const options = {
            style: "decimal",
            minimumFractionDigits: _min_digits,
            maximumFractionDigits: _min_digits,
            useGrouping: true,
        };
        return new Intl.NumberFormat(locales, options).format(num_val);
    };
    website["percent"] = function (num_val, min_digits) {
        let _min_digits = 2;
        if (null != min_digits)
            _min_digits = min_digits;
        const locales = "zh-tw";
        const options = {
            style: "percent",
            minimumFractionDigits: _min_digits,
            maximumFractionDigits: _min_digits,
            useGrouping: false,
        };
        return new Intl.NumberFormat(locales, options).format(num_val);
    };
    website["float"] = function (num_val, min_digits) {
        let _min_digits = 2;
        if (null != min_digits)
            _min_digits = min_digits;
        const locales = "zh-tw";
        const options = {
            style: "decimal",
            minimumFractionDigits: _min_digits,
            maximumFractionDigits: _min_digits,
            useGrouping: false,
        };
        return new Intl.NumberFormat(locales, options).format(num_val);
    };
})();
(function () {
    website["replace_all"] = function (str, scan_str, fix_str) {
        return str.split(scan_str).join(fix_str);
    };
})();
(function () {
    website["hash_string"] = function () {
        return {
            sha_256: function (plainText) {
                const def = $.Deferred();
                const text_byte_buffer = new TextEncoder().encode(plainText);
                const hashBuffer = window.crypto.subtle.digest('sha-256', text_byte_buffer);
                hashBuffer.then(function (hash_byte) {
                    const hashArray = Array.from(new Uint8Array(hash_byte));
                    const digest = hashArray.map(b => padStart(b.toString(16), 2, '0')).join('');
                    def.resolve(digest);
                }, function (e) {
                    console.log(e);
                    def.reject(e);
                });
                function padStart(str, targetLength, padString) {
                    targetLength = targetLength >> 0;
                    padString = String(padString || ' ');
                    if (str.length > targetLength) {
                        return String(str);
                    }
                    else {
                        targetLength = targetLength - str.length;
                        if (targetLength > padStart.length) {
                            padString += padString.repeat(targetLength / padString.length);
                        }
                        return padString.slice(0, targetLength) + String(str);
                    }
                }
                return def;
            },
            hmac_sha_256: function (plainText, private_key) {
                const def = $.Deferred();
                const encoder = new TextEncoder();
                const pkey_str = encoder.encode(private_key);
                const key_gen = window.crypto.subtle.importKey("raw", pkey_str, { name: "HMAC", hash: "SHA-256" }, false, ["sign"]);
                key_gen.then(function (key) {
                    const text_byte = encoder.encode(plainText);
                    const signature = window.crypto.subtle.sign("HMAC", key, text_byte);
                    signature.then(function (sign_data) {
                        const hashArray = Array.from(new Uint8Array(sign_data));
                        const hashHex = website["base64_url_enc_byte"](hashArray);
                        def.resolve(hashHex);
                    }, function (e) {
                        def.reject(e);
                    });
                }, function (e) {
                    def.reject(e);
                });
                return def;
            },
        };
    };
})();
(function () {
    website["aes_gcm"] = function () {
        const text_encoder = new TextEncoder();
        const text_decoder = new TextDecoder("utf-8");
        function build_aes_key(key_byte) {
            const def = $.Deferred();
            crypto.subtle.importKey("raw", key_byte, "aes-gcm", false, ["encrypt", "decrypt"]).then(function (key) {
                def.resolve(key);
            }, function (e) {
                console.log(e.message);
                def.reject(e);
            });
            return def;
        }
        function build_key_pair(plain_text) {
            const def = $.Deferred();
            (function () {
                const res = { key: "", key_byte: new ArrayBuffer(1), iv: "", iv_byte: new ArrayBuffer(1) };
                website.hash_string().sha_256(plain_text).done(function (hash_str) {
                    const key_str = hash_str.substr(0, 32);
                    const key_byte = text_encoder.encode(key_str);
                    res["key"] = key_str;
                    res["key_byte"] = key_byte;
                    website.hash_string().sha_256(hash_str).done(function (iv_str) {
                        const iv_byte = text_encoder.encode(iv_str);
                        res["iv"] = iv_str;
                        res["iv_byte"] = iv_byte;
                        def.resolve(res);
                    });
                });
            })();
            return def;
        }
        return {
            encrypt_string: function (plain_text, private_key) {
                const def = $.Deferred();
                build_key_pair(private_key).done(function (key_pair) {
                    build_aes_key(key_pair.key_byte).done(function (aes_key) {
                        const data_byte = text_encoder.encode(plain_text);
                        const enc_def = crypto.subtle.encrypt({ name: "aes-gcm", iv: key_pair.iv_byte }, aes_key, data_byte);
                        enc_def.then(function (enc_byte) {
                            const b64_enc_str = website["base64_url_enc_byte"](enc_byte);
                            def.resolve(b64_enc_str);
                        }, function (e) {
                            console.log("aes_exception: " + e.message);
                            def.reject(e);
                        });
                    });
                });
                return def;
            },
            decrypt_string: function (b64_enc_str, private_key) {
                const def = $.Deferred();
                build_key_pair(private_key).done(function (key_pair) {
                    build_aes_key(key_pair.key_byte).done(function (aes_key) {
                        const enc_byte = website["base64_url_dec_byte"](b64_enc_str);
                        const dec_def = crypto.subtle.decrypt({ name: "aes-gcm", iv: key_pair.iv_byte }, aes_key, enc_byte);
                        dec_def.then(function (dec_byte) {
                            const dec_str = text_decoder.decode(dec_byte);
                            def.resolve(dec_str);
                        }, function (e) {
                            console.log("aes_exception: " + e.message);
                            def.reject(e);
                        });
                    });
                });
                return def;
            },
            get_iv: function (private_key) {
                const def = $.Deferred();
                build_key_pair(private_key).done(function (key_pair) {
                    def.resolve(key_pair.iv);
                });
                return def;
            }
        };
    };
})();
(function () {
    website["observer"] = function (_process_fn) {
        class DefaultObserver {
            update(data) {
                if (null != _process_fn && (typeof _process_fn === 'function')) {
                    _process_fn(data);
                }
                else {
                    console.error("observer 的 _process_fn 未定義為 function，無法正常接收監聽內容回傳");
                }
            }
        }
        return new DefaultObserver();
    };
    website["subject"] = function () {
        class Subject {
            constructor() {
                this.observers = [];
            }
            subscribe(observer) {
                this.observers.push(observer);
            }
            unsubscribe(observer) {
                const observerIndex = this.observers.indexOf(observer);
                if (observerIndex > -1) {
                    this.observers.splice(observerIndex, 1);
                }
            }
            notify(data) {
                this.observers.forEach(observer => observer.update(data));
            }
        }
        return new Subject();
    };
})();
(function () {
    website["app_name"] = null;
    (function () {
        if (window.location.protocol.indexOf("http") > -1) {
            const path_name = window.location.pathname;
            const path_arr = path_name.split("/");
            const tmp_arr = [];
            while (1 < path_arr.length) {
                const tmp = path_arr.pop();
                if (null == tmp || 0 == tmp.length) {
                    if (path_arr.length > 0)
                        continue;
                    break;
                }
                tmp_arr.unshift(tmp);
            }
            website.app_name = "/" + tmp_arr[0];
        }
        else {
            const path_href = window.location.href;
            const path_arr = path_href.split("/");
            path_arr.pop();
            website.app_name = path_arr.join("/");
        }
        if ("/undefined" == website.app_name)
            website.app_name = "/testasync";
    })();
    const arr = [];
    (function () {
        arr.push(website["app_name"] + "/js/jquery/jquery.min.js");
        arr.push(website["app_name"] + "/js/axios/axios.min.js");
    })();
    (function () {
        let tmpPath = null;
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
                    website.script(website["app_name"] + "/js/index.js", function () {
                        website["index_fn"]();
                    });
                }
                break;
        }
    };
})();
