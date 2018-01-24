package framework.web.executor;

import com.alibaba.fastjson.JSONObject;
import framework.observer.Handler;
import framework.observer.Message;
import framework.web.context.AsyncActionContext;
import framework.web.handler.RequestHandler;

import java.util.ArrayList;

public class WebAppServiceExecutor {

    private AsyncActionContext requestContext;

    public WebAppServiceExecutor(AsyncActionContext asyncActionContext) {
        this.requestContext = asyncActionContext;
    }

    public void startup() {
        ArrayList<RequestHandler> handlers = getHandlerChain();
        if(handlers.size() > 0) {
            try {
                handlers.get(0).startup(requestContext);
            } catch (Exception e) {
                // 個別應用實例尚未到達 requestContext.complete() 而出錯時會進入此處
                e.printStackTrace();
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("error_code", "500");
                    obj.put("status", "fail");
                    obj.put("msg_eng", "server_side_exception_error");
                    obj.put("msg_zht", "伺服器端服務執行過程中發生例外錯誤");
                    requestContext.printToResponse(obj.toJSONString(), new Handler(){
                        @Override
                        public void handleMessage(Message m) {
                            super.handleMessage(m);
                            requestContext.complete();
                        }
                    });
                } catch (Exception ex) {
                    // ex.printStackTrace();
                }
            }
        } else {
            // 尚未成功建立責任鏈時
            JSONObject obj = new JSONObject();
            obj.put("error_code", "500");
            obj.put("status", "");
            obj.put("msg_eng", "server_side_service_node_is_null");
            obj.put("msg_zht", "伺服器端尚未建立服務節點");
            requestContext.printToResponse(obj.toJSONString(), new Handler(){
                @Override
                public void handleMessage(Message m) {
                    super.handleMessage(m);
                    requestContext.complete();
                }
            });
        }
    }

    /**
     * 此處重新建立各個 Handler 新實例的用意在於執行緒安全，
     * 雖然 Executor 層級已經是新實例，但如果 Handler 層級沒有採用新實例，
     * 而是一律採用初始化建立的實例時，會發生 AsyncContext 執行緒不安全的情況，
     * 使用者有可能使用到對方的請求責任鏈實例，進而發生短時間內可能重複 complete 的錯誤，
     * 藉由此方法修正該錯誤，加強套件功能的隔離性與穩健度
     */
    private ArrayList<RequestHandler> getHandlerChain() {
        ArrayList<RequestHandler> handlers = WebAppServicePoolStatic.getInstance().prototype(); // 物件範本
        ArrayList<RequestHandler> runHandlers = new ArrayList<>(); // 新實例容器
        // 建立新實例，因為是由 ArrayList 實作所以經過 foreach 取出時順序是不變的
        for (RequestHandler rawHandler : handlers) {
            try {
                RequestHandler newHandler = rawHandler.getClass().getConstructor().newInstance(); // jdk 9 update
                runHandlers.add(newHandler);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 重新建立新實例間的責任鏈關係（requestHandler 要知道是否還有下一位處理者）
        if(runHandlers.size() > 0) {
            for(int i = 0, len = runHandlers.size(); i < len; i++) {
                RequestHandler runHandler = runHandlers.get(i);
                if(i < (len - 1)) {
                    // 如果不是最後一個 requestHandler 則設定具有的下一個 requestHandler
                    runHandler.setNextHandler(runHandlers.get(i+1));
                }
            }
        }
        return runHandlers;
    }

}
