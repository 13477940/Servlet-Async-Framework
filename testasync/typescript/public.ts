// 通用腳本 - modify:2023-01-05

// namespace define
var debug_mode: boolean = true; // for try-catch, scope: ajax
var website: any = window.website || {};
var $: any = window.$ || null; // jquery
var axios: any = window.axios || null;

// script loader
(function(){
    website["script"] = function(script_url: string, ready_fn: any) {
        // https://developer.mozilla.org/zh-TW/docs/Web/API/Document/createElement
        // https://developer.mozilla.org/zh-TW/docs/Web/API/Document/readyState
        // https://developer.mozilla.org/en-US/docs/Web/API/Node/appendChild
        (function(){
            let timer: any = null;
            // loop check page ready status.
            exec_load_fn();
            // 載入腳本實作
            function exec_load_fn() {
                if(null != timer) clearTimeout(timer);
                timer = setTimeout(function(){
                    if("complete" == document.readyState) {
                        load_script_fn(script_url, ready_fn);
                    } else {
                        exec_load_fn();
                    }
                }, 60);
            }
        })();
    };
    function load_script_fn(script_url: any, ready_fn: any) {
        if(null == ready_fn) ready_fn = function(){}; // if null ready_fn
        const framework_label = "my_script_loader";
        let container_div = document.querySelector("div[_framework_key="+framework_label+"]");
        (function(){
            if(null == container_div) {
                container_div = document.createElement('div');
                container_div.setAttribute("_framework_key", framework_label);
                document.body.appendChild(container_div);
            }
        })();
        // 檢查是否需要同時載入的 script，同時載入以 array 形式帶入
        // 藉由當下計數器與目標計數器達成陣列腳本列表載入狀態確認
        let catch_count = 0;
        let target_count = 0;
        (function(){
            if(Array.isArray(script_url)) {
                target_count = script_url.length;
                // 當腳本路徑為陣列載入時會以讀取最久的腳本內容作為回應時間點
                for(let url_index in script_url) {
                    let url = script_url[url_index];
                    rm_rep_script(url);
                    append_script_tag(url, container_div, catch_script_done, catch_script_fail);
                }
            } else {
                target_count = 1;
                rm_rep_script(script_url);
                append_script_tag(script_url, container_div, catch_script_done, catch_script_fail);
            }
        })();
        function catch_script_done(evt: any) {
            catch_count++;
            if(target_count == catch_count) ready_fn();
        }
        function catch_script_fail(evt: any) {
            catch_count++;
            if(target_count == catch_count) ready_fn();
            console.error(evt);
        }
    }
    function append_script_tag(url: any, container_div: any, ready_fn: any, error_fn: any) {
        let scriptTag = document.createElement('script');
        scriptTag.src = url;
        if(null != ready_fn) scriptTag.onload = ready_fn;
        if(null != error_fn) scriptTag.onerror = error_fn;
        container_div.append(scriptTag);
    }
    // https://developer.mozilla.org/en-US/docs/Web/API/Document/querySelectorAll
    // 當重複載入同個 script 路徑則將舊的 tag 刪除
    let prefix: any = [];
    (function(){
        prefix.push(window.location.protocol);
        prefix.push("//");
        prefix.push(window.location.hostname);
    })();
    function rm_rep_script(url: string) {
        let elems = document.querySelectorAll("script");
        let chk_str = prefix.join("") + url;
        for(let i = 0, len = elems.length; i < len; i++) {
            let elem = elems[i];
            if(elem.src.includes(chk_str)) {
                elem.remove();
            }
        }
    }
})();

// 亂數產生器
(function(){
    website["randomString"] = function(len: any) {
        const t = "abcdefghijklmnopqrstuvwxyz0123456789";
        let r = [];
        let l = 16; // default
        let tLen = t.length;
        if(null != len) l = len;
        for(let i = 0; i < l; i++) {
            const c = t.charAt( Math.floor( Math.random() * tLen ) );
            r.push(c);
        }
        return r.join("");
    };
})();

// axios module
(function(){
    // get
    (function(){
        website["get"] = function(reqObj: any) {
            const def = $.Deferred();
            if (null != reqObj["header"] && false == Array.isArray(reqObj["header"])) {
                console.error("header 參數必須使用 array 型態");
                def.reject();
                return;
            }
            const headers: any = {};
            (function(){
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
            (function(){
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
                    function (data: any) { return data; }
                ],
                headers: headers,
                params: params
            };
            axios.get(reqObj["url"], config).then(function(response: any) {
                let respObj = {
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
    // post - application/x-www-form-urlencoded
    (function(){
        website["post"] = function(reqObj: any) {
            const def = $.Deferred();
            if (null != reqObj["header"] && false == Array.isArray(reqObj["header"])) {
                console.error("header 參數必須使用 array 型態");
                def.reject();
                return;
            }
            const headers: any = {};
            (function(){
                let arr = reqObj["header"];
                for (let index in arr) {
                    let obj = arr[index];
                    let key = obj["key"];
                    let value = obj["value"];
                    headers[key] = value;
                }
                (function(){
                    headers["content-type"] = "application/x-www-form-urlencoded;charset=utf-8";
                })();
            })();
            if (null != reqObj["data"] && false == Array.isArray(reqObj["data"])) {
                console.error("data 參數必須使用 array 型態");
                def.reject();
                return;
            }
            const params = new URLSearchParams();
            (function(){
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
                    function(data: any) { return data; }
                ],
                headers: headers
            };
            axios.post(reqObj["url"], params, config).then(function(response: any) {
                let respObj = {
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
    // post - application/json
    (function(){
        website["post_json"] = function(reqObj: any) {
            const def = $.Deferred();
            if (null != reqObj["header"] && false == Array.isArray(reqObj["header"])) {
                console.error("header 參數必須使用 array 型態");
                def.reject();
                return;
            }
            const headers: any = {};
            (function () {
                let arr = reqObj["header"];
                for (let index in arr) {
                    let obj = arr[index];
                    let key = obj["key"];
                    let value = obj["value"];
                    headers[key] = value;
                }
                (function(){
                    headers["content-type"] = "application/json;charset=utf-8";
                })();
            })();
            axios.post(reqObj["url"], reqObj["text"]).then(function(response: any) {
                let respObj = {
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
    // post - multipart/form-data
    (function(){
        website["post_form_data"] = function(reqObj: any) {
            const def = $.Deferred();
            // req header
            if(null != reqObj["header"] && false == Array.isArray(reqObj["header"])) {
                console.error("header 參數必須使用 array 型態");
                def.reject();
                return;
            }
            const headers: any = {};
            (function(){
                let arr = reqObj["header"];
                for(let index in arr) {
                    let obj = arr[index];
                    let key = obj["key"];
                    let value = obj["value"];
                    headers[key] = value;
                }
                (function(){
                    headers["content-type"] = "multipart/form-data";
                })();
            })();
            // req params
            if(null != reqObj["data"] && false == Array.isArray(reqObj["data"])) {
                console.error("data 參數必須使用 array 型態");
                def.reject();
                return;
            }
            const formData = new FormData();
            (function(){
                if(null != reqObj["data"]) {
                    let arr = reqObj["data"];
                    for(let index in arr) {
                        let obj = arr[index];
                        formData.append(obj["key"], obj["value"]);
                    }
                }
            })();
            const config = {
                transformResponse: [function(data: any){ return data; }], // 修正 response 回傳內容格式
                headers: headers,
                // maxContentLength: 20000000,
                onUploadProgress: function(progressEvent: any) {
                    let percentCompleted = Math.round((progressEvent.loaded * 100) / progressEvent.total);
                    let percentStr = String(percentCompleted); // percent number
                    let respObj = {
                        status: "upload_progress",
                        progress_value: percentStr
                    };
                    def.notify(respObj);
                }
            };
            // form-data 格式請使用 post method
            axios.post(reqObj["url"], formData, config).then(function(response: any) {
                let respObj = {
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
(function(){
    website["dialog"] = function(initObj: any) {
        const def = $.Deferred();
        const dialogId = "_dialog_"+website.randomString(16);
        const dialogElem = $(buildDialogHtml());
        (function(){
            dialogElem.attr("modal_dialog_ssid", dialogId);
            dialogElem.css("z-index", "10");
            dialogElem.css("background-color", "rgba(90,90,90,0.5)");
            if(null != initObj["content"] && initObj["content"].length > 0) {
                // var content_elem = $(initObj["content"]);
                dialogElem.find("div[modal_dialog_key=wrap]").append(initObj["content"]);
            }
        })();
        $("body").append(dialogElem);
        dialogElem.css("display", "flex");
        const dialogObj = {
            dialog: dialogElem.find("div[modal_dialog_key=wrap]"),
            overlay: dialogElem,
            close: function(){
                dialogElem.remove();
            }
        };
        // esc key setting（要配合 tabindex 設定）
        (function(){
            let enable_esc_key = "true";
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
        const overlay_elem = $("<div modal_dialog_key='overlay' tabindex='0'></div>");
        (function(){
            overlay_elem.css("display", "none");
            overlay_elem.css("align-items", "stretch");
            overlay_elem.css("justify-content", "stretch");
            overlay_elem.css("position", "fixed");
            overlay_elem.css("top", "0px");
            overlay_elem.css("left", "0px");
            overlay_elem.css("width", "100vw");
            overlay_elem.css("height", "100vh");
            overlay_elem.css("overflow", "auto");
            overlay_elem.css("backdrop-filter", "blur(1px)"); // 毛玻璃效果
        })();
        // 藉由 flex 特性，達到水平與垂直置中，若高度超過視器高度則啟用 overflow
        (function(){
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

// hyperlinker
(function(){
    website["redirect"] = function(url: string, onCache: boolean) {
        if(null == onCache) onCache = true; // GET 預設是快取狀態
        let targetURL = null;
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
            const ts = Date.now();
            location.href = targetURL + "?ei=" + ts;
        }
    };
})();

// native javascript base64 url safe
(function(){
    website["base64_encode"] = function( content: any ) {
        if(null == content) {
            console.error("b64 encode content is null!");
            return null;
        }
        const text_encoder = new TextEncoder();
        const aMyUTF8Input = text_encoder.encode(content);
        return base64EncArr(aMyUTF8Input);
    }
    website["base64_url_encode"] = function( content: any ) {
        return url_safe_withoutPadding( website["base64_encode"](content) );
    };
    website["base64_decode"] = function( content: any ) {
        if(null == content) {
            console.error("b64 decode content is null!");
            return null;
        }
        const text_decoder = new TextDecoder();
        const aMyUTF8Output = base64DecToArr(content, 1);
        return text_decoder.decode(aMyUTF8Output);
    };
    website["base64_url_decode"] = function( content: any ) {
        return website["base64_decode"](content);
    };

    // for byte
    website["base64_enc_byte"] = function(src_byte: any) {
        const tmp_arr = new Uint8Array(src_byte);
        return base64EncArr(tmp_arr);
    };
    website["base64_dec_byte"] = function(src_byte: any) {
        return base64DecToArr(src_byte, 1);
    };
    website["base64_url_enc_byte"] = function(src_byte: any) {
        return url_safe_withoutPadding(website["base64_enc_byte"](src_byte));
    };
    website["base64_url_dec_byte"] = function(src_byte: any) {
        return website["base64_dec_byte"](src_byte);
    };

    // https://jsfiddle.net/magikMaker/7bjaT/
    function url_safe_withoutPadding(str: any) {
        return str.replace(/\+/g, '-').replace(/\//g, '_').replace(/\=+$/, '');
    }
    // https://jsfiddle.net/magikMaker/7bjaT/
    function decode_url_safe(str: any) {
        return str.replace(/-/g, '+').replace(/_/g, '/');
    }

    /* Base64 string to array encoding */
    function uint6ToB64(nUint6: any) {
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

    function base64EncArr(aBytes: any) {
        let nMod3 = 2;
        let sB64Enc = "";

        const nLen = aBytes.length;
        let nUint24 = 0;
        for (let nIdx = 0; nIdx < nLen; nIdx++) {
            nMod3 = nIdx % 3;
            if (nIdx > 0 && ((nIdx * 4) / 3) % 76 === 0) {
                // sB64Enc += "\r\n";
            }
            nUint24 |= aBytes[nIdx] << ((16 >>> nMod3) & 24);
            if (nMod3 === 2 || aBytes.length - nIdx === 1) {
                sB64Enc += String.fromCodePoint(
                    uint6ToB64((nUint24 >>> 18) & 63),
                    uint6ToB64((nUint24 >>> 12) & 63),
                    uint6ToB64((nUint24 >>> 6) & 63),
                    uint6ToB64(nUint24 & 63)
                );
                nUint24 = 0;
            }
        }
        return ( sB64Enc.substring(0, sB64Enc.length - 2 + nMod3) + (nMod3 === 2 ? "" : nMod3 === 1 ? "=" : "==") );
    }

    // Array of bytes to Base64 string decoding
    function b64ToUint6(nChr: any) {
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

    // nBlocksSize -> 1 = utf-8
    function base64DecToArr(sBase64: any, nBlocksSize: any) {
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
    website["print"] = function( elem: any, width: any, height: any ) {
        // default print content preview
        // window.open('tab_url', 'tab_name', 'width=800,height=600');
        var w_width = $(window).width() * 0.9;
        if(null != width) w_width = width;
        var w_height = $(window).height() * 0.9;
        if(null != w_height) w_height = height;
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
            if( elem.prop("outerHTML").includes("<img") ) b_need_load = true;
            if( b_need_load ) {
                mywindow.onload = function() { // wait until all resources loaded
                    mywindow.focus(); // necessary for IE >= 10
                    mywindow.print(); // change window to mywindow
                    mywindow.close(); // change window to mywindow
                };
            } else {
                mywindow.focus(); // necessary for IE >= 10
                mywindow.print();
                mywindow.close();
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
// 主要是對於 windows os 時有效
(function(){
    website["scroll_width"] = function() {
        var scr = null;
        var inn = null;
        var wNoScroll = 0;
        var wScroll = 0;
        // Outer scrolling div
        scr = document.createElement('div');
        var div_id = "_scroll_test_" + website.randomString(16);
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
    website["float"] = function(number: any, fractionDigits: any) {
        var def = fractionDigits || 2;
        return Math.round(number* Math.pow(10, def))/ Math.pow(10, def);
    };
})();

// https://stackoverflow.com/questions/494035/how-do-you-use-a-variable-in-a-regular-expression
(function(){
    website["replace_all"] = function(str: any, scan_str: any, fix_str: any) {
        return str.split(scan_str).join(fix_str);
    };
})();

// AES GCM
(function(){
    website["aes_gcm"] = function(){
        // byte encode, decode
        const text_encoder = new TextEncoder();
        const text_decoder = new TextDecoder("UTF-8");

        function build_aes_key(key_byte: any) {
            const def = $.Deferred();
            // importKey(format, keyData, algorithm, extractable, keyUsages)
            crypto.subtle.importKey (
                "raw",
                key_byte,
                "aes-gcm",
                false,
                ["encrypt","decrypt"]
            ).then(function(key) {
                def.resolve(key);
            }, function(e){
                console.log(e.message);
                def.reject(e);
            });
            return def;
        }

        // key 長度會影響 gcm-128 或 gcm-256 結果
        // An 8 bit array with 16 elements = 128 bits.
        // An 8 bit array with 32 elements = 256 bits.
        // 將明文密鑰經由自定義流程建立流程創建鑰匙組（private_key+iv）
        function build_key_pair(plain_text: any) {
            const def = $.Deferred();
            (function(){
                const res = { key: "", key_byte: new ArrayBuffer(1), iv: "", iv_byte: new ArrayBuffer(1) };
                stringToSha256(plain_text).done(function(hash_str: any){
                    const key_str = hash_str.substr(0, 32); // 16 or 32
                    const key_byte = text_encoder.encode(key_str);
                    res["key"] = key_str;
                    res["key_byte"] = key_byte;
                    stringToSha256(hash_str).done(function(iv_str: any){
                        const iv_byte = text_encoder.encode(iv_str);
                        res["iv"] = iv_str;
                        res["iv_byte"] = iv_byte;
                        def.resolve(res);
                    });
                });
            })();
            return def;
        }

        // PlainText to SHA256
        function stringToSha256(plainText: any) {
            const def = $.Deferred();
            const textAsBuffer = new TextEncoder().encode(plainText);
            const hashBuffer = window.crypto.subtle.digest('SHA-256', textAsBuffer);
            hashBuffer.then(function(hash_byte){
                const hashArray = Array.from(new Uint8Array(hash_byte));
                const digest = hashArray.map(b => padStart(b.toString(16), 2, '0')).join('');
                def.resolve(digest);
            }, function(e){
                console.log(e);
                def.reject(e);
            });
            // String.prototype.padStart 相容
            // https://www.freecodecamp.org/news/how-does-string-padstart-actually-work-abba34d982e/
            function padStart(str: any, targetLength: any, padString: any) {
                // floor if number of convert non-number to 0;
                targetLength = targetLength >> 0;
                padString = String(padString || ' ');
                if(str.length > targetLength) {
                    return String(str);
                } else {
                    targetLength = targetLength - str.length;
                    if(targetLength > padStart.length) {
                        // append to original to ensure we are longer than needed
                        padString += padString.repeat(targetLength / padString.length);
                    }
                    return padString.slice(0, targetLength) + String(str);
                }
            }
            return def;
        }

        // public call functions
        return {
            encrypt_string: function(plain_text: any, private_key: any) {
                const def = $.Deferred();
                build_key_pair(private_key).done(function(key_pair: any){
                    build_aes_key(key_pair.key_byte).done(function(aes_key: any){
                        const data_byte = text_encoder.encode(plain_text);
                        const enc_def = crypto.subtle.encrypt({name: "aes-gcm", iv: key_pair.iv_byte}, aes_key, data_byte);
                        enc_def.then(function(enc_byte){
                            const b64_enc_str = website["base64_url_enc_byte"](enc_byte);
                            def.resolve(b64_enc_str);
                        }, function(e){
                            console.log(e.message);
                            def.reject(e);
                        });
                    });
                });
                return def;
            },
            decrypt_string: function(b64_enc_str: any, private_key: any) {
                const def = $.Deferred();
                build_key_pair(private_key).done(function(key_pair: any){
                    build_aes_key(key_pair.key_byte).done(function(aes_key: any){
                        const enc_byte = website["base64_url_dec_byte"](b64_enc_str);
                        const dec_def = crypto.subtle.decrypt({name: "aes-gcm", iv: key_pair.iv_byte}, aes_key, enc_byte);
                        dec_def.then(function(dec_byte){
                            const dec_str = text_decoder.decode(dec_byte);
                            def.resolve(dec_str);
                        }, function(e){
                            console.log(e.message);
                            def.reject(e);
                        });
                    });
                });
                return def;
            },
            get_iv: function(private_key: any) {
                const def = $.Deferred();
                build_key_pair(private_key).done(function(key_pair: any){
                    def.resolve(key_pair.iv);
                });
                return def;
            }
        };
    };
})();
