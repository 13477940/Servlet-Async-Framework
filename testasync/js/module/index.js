var testasync = {};
(function() {
    testasync["readyFn"] = function() {
        $("#upok").on("click", function() {
            var myfile = $("#upfile")[0]["files"][0];
            testasync.postData();
            testasync.postFormData(myfile);
        });
    };
    testasync["postData"] = function() {
        var ajax = $.ajax({
            async: true,
            type: "post",
            url: "/testasync/index",
            data: {
                page: "123",
                a: "123",
                b: "456",
                c: "789",
                d: "中文字"
            },
            beforeSend: function() {}
        });
        ajax.done(doneFn);
        ajax.fail(failFn);

        function doneFn(respd) {
            console.log(respd);
        }

        function failFn() {}
    };
    testasync["postFormData"] = function(myfile) {
        var fData = new FormData();

        if (null == myfile) {
            fData.append("page", "page");
            fData.append("api_type", "member_login");
            fData.append("ts", "1495423569106");
            fData.append("token", "7c4ffb9a4b227fd13e1b6bc574572862");
            fData.append("password", "TestPassword");
            fData.append("unicode_test", "中文字測試");

            (function() {
                var ajax = $.ajax({
                    async: true,
                    type: "post",
                    url: "/testasync/index",
                    data: fData,
                    processData: false,
                    contentType: false,
                    beforeSend: function() {}
                });
                ajax.done(doneFn);
                ajax.fail(failFn);

                function doneFn(respd) {
                    console.log(respd);
                }

                function failFn() {}
            }());
        } else {
            fData.append("page", "page");
            fData.append("api_type", "member_login");
            fData.append("ts", "1495423569106");
            fData.append("token", "7c4ffb9a4b227fd13e1b6bc574572862");
            fData.append("password", "TestPassword");
            fData.append("unicode_test", "中文字測試");
            fData.append("myfile", myfile);

            // 具有上傳進度值的 jQuery AJAX
            // https://segmentfault.com/a/1190000008791342
            (function() {
                var ajax = $.ajax({
                    async: true,
                    type: "post",
                    url: "/testasync/index",
                    data: fData,
                    processData: false,
                    contentType: false,
                    xhr: function() {
                        var myXhr = $.ajaxSettings.xhr();
                        if (myXhr.upload) {
                            myXhr.upload.addEventListener('progress', function(e) {
                                if (e.lengthComputable) {
                                    var percent = Math.floor(e.loaded / e.total * 100);
                                    if (percent <= 100) {
                                        console.log(percent);
                                    }
                                    if (percent >= 100) {
                                        console.log("upload done.");
                                    }
                                }
                            }, false);
                        }
                        return myXhr;
                    },
                    beforeSend: function() {}
                });
                ajax.done(doneFn);
                ajax.fail(failFn);

                function doneFn(respd) {
                    console.log(respd);
                }

                function failFn() {}
            }());
        }
    };
}());
$(testasync["readyFn"]);
