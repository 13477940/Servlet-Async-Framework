(function(){
    website["index_fn"] = function(){
        // ajax worker example
        (function(){
            $("#btn_select_file").on("click", function(){
                $("#upfile").click();
            });
        })();
        (function(){
            $("#label_select_file").html("[未選擇檔案]");
            $("#upfile").on("change", function(){
                const targetFile = $("#upfile")[0]["files"][0];
                if(null != targetFile) {
                    $("#label_select_file").html(targetFile.name);
                } else {
                    $("#label_select_file").html("[未選擇檔案]");
                }
            });
        })();
        (function(){
            $("#btn_upload_submit").on("click", function(){
                const targetFile = $("#upfile")[0]["files"][0];
                (function(){
                    const reqObj = {
                        url: "/testasync/index",
                        data: [
                            { key: "a", value: "123" },
                            { key: "b", value: "ABC" },
                            { key: "c", value: "中文測試" }
                        ]
                    };
                    if(null != targetFile) {
                        reqObj.data.push({
                            key: "myfile",
                            value: targetFile
                        });
                    }
                    website.post_form_data(reqObj)
                        .progress(function(prog){
                            console.log(prog);
                        })
                        .done(function(respd){
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
                    content: $("div[ui_key=template]").find("div[ui_key=dialog_msg]").prop("outerHTML")
                }).done(function(dialog_elem){
                    console.log(dialog_elem);
                    (function(){
                        dialog_elem.dialog.find("button[ui_key=btn_cancel]").on("click", function(){
                            dialog_elem.close();
                        });
                    })();
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
                        { key: "a", value: "123" },
                        { key: "b", value: "ABC" },
                        { key: "c", value: "中文測試" },
                        { key: "d", value: "/// //aaa// a//cc[]///" }
                    ],
                    header: [
                        { key: "my-auth", value: "aaabbbcccdddeeefffggg_hi" }
                    ]
                }).done(function(respd){
                    console.log(respd);
                });
            });
        })();
        (function(){
            $("button[ui_key=btn_test_post]").on("click", function(){
                website.post({
                    url: "/testasync/index",
                    data: [
                        { key: "a", value: "123" },
                        { key: "b", value: "ABC" },
                        { key: "c", value: "中文測試" },
                        { key: "d", value: "/// //aaa// a//cc[]///" }
                    ],
                    header: [
                        { key: "my-auth", value: "test_post_123456_hi" }
                    ]
                }).done(function(respd){
                    console.log(respd);
                });
            });
        })();
        (function(){
            function asyncTest() {
                const reqObj = {
                    url: "/testasync/index",
                    data: [
                        { key: "A", value: "123" },
                        { key: "B", value: "200" },
                        { key: "C", value: "300" }
                    ]
                };
                website.get(reqObj).done(function(respd){
                    const obj = JSON.parse(respd);
                    console.log(obj);
                });
            }
            asyncTest();
            /*
            for(var i = 0, len = 4000; i < len; i++) {
                asyncTest();
            }*/
        })();
        // websocket client
        (function(){
            function getRootUri() {
                let ws_protocal = "ws://";
                let ws_port = ":80";
                if(location.protocol.indexOf("https") > -1) {
                    ws_protocal = "wss://";
                    ws_port = ":443";
                }
                return ws_protocal + location.host + ws_port;
            }
            const socketList = [];
            const ws_uri = getRootUri() + "/testasync/websocket";
            const web_socket = new WebSocket(ws_uri);
            socketList.push(web_socket);
            web_socket.onopen = function(evt) {
                // console.log(evt);
                socketReady();
            };
            web_socket.onmessage = function(evt) {
                const eventObj = JSON.parse(evt.data);
                // sendMsg(eventObj);
                console.log(eventObj);
            };
            web_socket.onerror = function(evt) {
                // console.log(evt);
                reconnectFn();
            };
            web_socket.onclose = function(evt) {
                // console.log(evt);
                reconnectFn();
            };
            function socketReady() {
                const tmpObj = { "cmd": "viewer_reg" };
                web_socket.send(JSON.stringify(tmpObj));
            }
            function reconnectFn() {
                setTimeout(function(){
                    const opt = socketList.shift();
                    opt.close();
                    // initWebSocket();
                }, 10000); // 10s
            }

            $("button[ui_key=btn_send_msg_to_ws]").on("click", function(){
                const input_elem = $("input[ui_key=send_ws_content]");
                web_socket.send(input_elem.val());
                input_elem.val("");
                alert("send socket message done.");
            });
        })();
    };
})();
