"use strict"
var website = window.website || {};
(function() {
    website["readyFn"] = function() {
		// ajax worker example
        $("#upok").on("click", function() {
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
		// pure javascript dialog example
		// https://www.w3schools.com/howto/howto_css_modals.asp
		(function(){
			function openDialog() {
				website.dialog({
					content: "<div style='position: relative; width: 400px;height: 400px;margin: auto 0px auto 0px;background-color: #fff;border-radius: 5px;'><span dialog_btn='close' style='position: absolute;top: 20px;right: 20px;'>&times;</span>TEST</div>"
				}).done(function(dialogElem){
					console.log(dialogElem);
					dialogElem.css("background-color", "rgba(190,190,190,0.5)");
					dialogElem.find("span[dialog_btn=close]").on("click", function(){
						dialogElem.remove();
					});
				});
			}
			$("button[ui_key=open_dialog]").on("click", function(){
				openDialog();
			});
		})();
		// console.log(website);
    };
}());
$(website["readyFn"]);
