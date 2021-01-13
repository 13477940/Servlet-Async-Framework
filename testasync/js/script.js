"use strict";var website=window.website||{},$=window.$||null,axios=window.axios||null;website.script=function(e,a){if(Array.isArray(e))for(var o=0,n=e.length;o<n;o++)t(e[o]);else t(e);function t(e){var o=document.createElement("script");o.src=e,o.onload=a,document.head.appendChild(o)}},website.randomString=function(e){var a="abcdefghijklmnopqrstuvwxyz0123456789",o=[],n=16,t=a.length;null!=e&&(n=e);for(var r=0;r<n;r++){var i=a.charAt(Math.floor(Math.random()*t));o.push(i)}return o.join("")},website.get=function(e){var a=$.Deferred();if(null!=e.header&&0==Array.isArray(e.header))return console.error("header 參數必須使用 array 型態"),void a.reject();var o={};if(function(){var a=e.header;for(var n in a){var t=a[n],r=t.key,i=t.value;o[r]=i}}(),null!=e.data&&0==Array.isArray(e.data))return console.error("data 參數必須使用 array 型態"),void a.reject();var n=new URLSearchParams;!function(){var a=e.data;for(var o in a){var t=a[o],r=t.key,i=t.value;n.append(r,i)}}();var t={transformResponse:[function(e){return e}],headers:o,params:n};return axios.get(e.url,t).then((function(e){var o={status:"done",status_code:e.status,data:e.data};a.resolve(o.data)})).catch((function(e){a.reject(e)})),a},website.post=function(e){var a=$.Deferred();if(null!=e.header&&0==Array.isArray(e.header))return console.error("header 參數必須使用 array 型態"),void a.reject();var o={};if(function(){var a=e.header;for(var n in a){var t=a[n],r=t.key,i=t.value;o[r]=i}}(),null!=e.data&&0==Array.isArray(e.data))return console.error("data 參數必須使用 array 型態"),void a.reject();var n=new URLSearchParams;!function(){var a=e.data;for(var o in a){var t=a[o],r=t.key,i=t.value;n.append(r,i)}}();var t={transformResponse:[function(e){return e}],headers:o};return axios.post(e.url,n,t).then((function(e){var o={status:"done",status_code:e.status,data:e.data};a.resolve(o.data)})).catch((function(e){a.reject(e)})),a},website.post_json=function(e){var a=$.Deferred();if(null!=e.header&&0==Array.isArray(e.header))return console.error("header 參數必須使用 array 型態"),void a.reject();var o={};return function(){var a=e.header;for(var n in a){var t=a[n],r=t.key,i=t.value;o[r]=i}}(),axios.post(e.url,e.text).then((function(e){var o={status:"done",status_code:e.status,data:e.data};a.resolve(o.data)})).catch((function(e){a.reject(e)})),a},website.post_form_data=function(e){var a=$.Deferred();if(null!=e.header&&0==Array.isArray(e.header))return console.error("header 參數必須使用 array 型態"),void a.reject();var o={};if(function(){var a=e.header;for(var n in a){var t=a[n],r=t.key,i=t.value;o[r]=i}}(),null!=e.data&&0==Array.isArray(e.data))return console.error("data 參數必須使用 array 型態"),void a.reject();var n=new FormData;!function(){if(null!=e.data){var a=e.data;for(var o in a){var t=a[o];n.append(t.key,t.value)}}}();var t={transformResponse:[function(e){return e}],headers:o,onUploadProgress:function(e){var o=Math.round(100*e.loaded/e.total),n={status:"upload_progress",progress_value:String(o)};a.notify(n)}};return axios.post(e.url,n,t).then((function(e){var o={status:"done",status_code:e.status,data:e.data};a.resolve(o.data)})).catch((function(e){a.reject(e)})),a},website.dialog=function(e){var a,o=$.Deferred(),n=website.randomString(16),t=$(((a=[]).push("<div modal_dialog_key='overlay' style='display: none;position: fixed;top: 0px;left: 0px;height: 100vh;width: 100vw;overflow: auto;'>"),a.push("<div modal_dialog_key='wrap' style='margin: auto'></div>"),a.push("</div>"),a.join("")));t.attr("modal_dialog_ssid",n),t.css("z-index","10"),t.css("background-color","rgba(245,245,245,0.5)"),null!=e.content&&e.content.length>0&&t.find("div[modal_dialog_key=wrap]").append(e.content),$("body").append(t),t.css("display","flex");var r={dialog:t.find("div[modal_dialog_key=wrap]"),overlay:t,close:function(){t.remove()}};return o.resolve(r),o},website.dropdown=function(e,a){var o,n=$.Deferred(),t=null,r=website.randomString(12);(o=[]).push("<div dropdown_ssid='"+r+"' ui_type='dropdown'>"),o.push("<div ui_type='dropdown_label' style='position: relative;z-index: 2;'>dropdown label</div>"),o.push("<div ui_type='dropdown_list' style='display: none;position: absolute;max-height: 400px;'></div>"),o.push("</div>");var i=(t=$(o.join(""))).find("div[ui_type=dropdown_list]");i.css("z-index","3"),function(){if(null!=a)for(var e=0,o=a.length;e<o;e++)i.append(a[e])}();var d=t.find("div[ui_type=dropdown_label]"),l=!1,s=$("<div dropdown_overlay='"+r+"' style='position: fixed;top: 0px;left: 0px;z-index: 1;width: 100vw;height: 100vh;'></div>");function u(){i.css("display","none"),$("div[dropdown_overlay='"+r+"']").remove(),l=!1}return d.on("click",(function(){l?u():(i.css("display","block"),$("body").append(s),s.on("click",(function(){u()})),l=!0)})),function(){e.append(t);var a={dropdown:t,label:d,list:i};n.resolve(a)}(),n},website.redirect=function(e,a){null==a&&(a=!0);var o=null;if(o=null==e?location.protocol+"//"+location.host+location.pathname:e,a)location.href=o;else{var n=Date.now();location.href=o+"?ei="+n}};$=window.$||null;!function(){var e,a=[];a.push("/testasync/js/jquery/jquery.min.js"),a.push("/testasync/js/axios/axios.min.js"),e=null,function o(){null!=(e=a.shift())?website.script(e,(function(){o()})):null==website.ready?console.error("該頁面不具有 window.website.ready 方法，無法完成初始化呼叫"):setTimeout((function(){website.ready()}),1)}()}(),website.ready=function(){var e;switch($("body").attr("page_key")){case"index":$("#btn_select_file").on("click",(function(){$("#upfile").click()})),$("#label_select_file").html("[未選擇檔案]"),$("#upfile").on("change",(function(){var e=$("#upfile")[0].files[0];null!=e?$("#label_select_file").html(e.name):$("#label_select_file").html("[未選擇檔案]")})),$("#btn_upload_submit").on("click",(function(){var e,a=$("#upfile")[0].files[0];e={url:"/testasync/index",data:[{key:"a",value:"100"},{key:"b",value:"ABC"},{key:"c",value:"國字測試"}]},null!=a&&e.data.push({key:"myfile",value:a}),website.post_form_data(e).progress((function(e){console.log(e)})).done((function(e){console.log(e)}))})),function(){function e(){website.dialog({content:"<div style='position: relative; width: 400px;height: 400px;margin: auto 0px auto 0px;background-color: #fff;border-radius: 5px;'><span dialog_btn='close' style='position: absolute;top: 20px;right: 20px;'>&times;</span>TEST</div>"}).done((function(e){console.log(e),e.overlay.css("background-color","rgba(190,190,190,0.5)"),e.dialog.find("span[dialog_btn=close]").on("click",(function(){e.close()}))}))}$("button[ui_key=btn_open_dialog]").on("click",(function(){e()}))}(),$("button[ui_key=btn_test_get]").on("click",(function(){website.get({url:"/testasync/index",data:[{key:"a",value:"100"},{key:"b",value:"ABC"},{key:"c",value:"國字測試"},{key:"d",value:"/// //aaa// a//cc[]///"}],header:[{key:"my-auth",value:"aaabbbcccdddeeefffggg_hi"}]}).done((function(e){console.log(e)}))})),$("button[ui_key=btn_test_post]").on("click",(function(){website.post({url:"/testasync/index",data:[{key:"a",value:"100"},{key:"b",value:"ABC"},{key:"c",value:"國字測試"},{key:"d",value:"/// //aaa// a//cc[]///"}]}).done((function(e){console.log(e)}))})),e=["<div style='padding: 15px 20px;color: #000;'>option 1</div>","<div style='padding: 15px 20px;color: #000;'>option 2</div>","<div style='padding: 15px 20px;color: #000;'>option 3</div>","<div style='padding: 15px 20px;color: #000;'>option 4</div>","<div style='padding: 15px 20px;color: #000;'>option 5</div>"],website.dropdown($("div[ui_key=btn_dropdown]"),e).done((function(e){console.log(e)})),function(){function e(){setTimeout((function(){var e={url:"/testasync/index",data:[{key:"A",value:"100"},{key:"B",value:"200"},{key:"C",value:"300"}]};website.get(e).done((function(e){var a=JSON.parse(e);console.log(a)}))}),1e3)}e()}()}};