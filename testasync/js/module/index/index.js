"use strict"
var website = window.website || {};
(function() {
    website["readyFn"] = function() {
		// ajax worker example
		(function(){
			$("#btn_select_file").on("click", function(){
				$("#upfile").click();
			});
		})();
		(function(){
			$("#label_select_file").html("[未選擇檔案]");
			$("#upfile").on("change", function(){
				var targetFile = $("#upfile")[0]["files"][0];
				if(null != targetFile) {
					$("#label_select_file").html(targetFile.name);
				} else {
					$("#label_select_file").html("[未選擇檔案]");
				}
			});
		})();
		(function(){
			$("#btn_upload_submit").on("click", function() {
				var targetFile = $("#upfile")[0]["files"][0];
				(function(){
					var reqObj = {
						url: "/testasync/index",
						data: [
							{ key: "a", value: "100" },
							{ key: "b", value: "ABC" },
							{ key: "c", value: "國字測試" }
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
					content: "<div style='position: relative; width: 400px;height: 400px;margin: auto 0px auto 0px;background-color: #fff;border-radius: 5px;'><span dialog_btn='close' style='position: absolute;top: 20px;right: 20px;'>&times;</span>TEST</div>"
				}).done(function(dialogObj){
					console.log(dialogObj);
					dialogObj.overlay.css("background-color", "rgba(190,190,190,0.5)");
					dialogObj.dialog.find("span[dialog_btn=close]").on("click", function(){
						dialogObj.close();
					});
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
						{ key: "a", value: "100" },
						{ key: "b", value: "ABC" },
						{ key: "c", value: "國字測試" },
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
						{ key: "a", value: "100" },
						{ key: "b", value: "ABC" },
						{ key: "c", value: "國字測試" },
						{ key: "d", value: "/// //aaa// a//cc[]///" }
					]
				}).done(function(respd){
					console.log(respd);
				});
			});
		})();
    };
}());
$(website["readyFn"]);
