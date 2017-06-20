package framework.executor;

import com.alibaba.fastjson.JSONObject;
import framework.context.AsyncActionContext;
import framework.handler.RequestHandler;

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
                e.printStackTrace();
                try {
                    JSONObject obj = new JSONObject();
                    obj.put("error_code", "500");
                    obj.put("status", "server_side_exception_error");
                    obj.put("msg_zht", "伺服器端服務執行過程中發生例外錯誤");
                    requestContext.printToResponse(obj.toJSONString());
                    requestContext.complete();
                } catch (Exception ex) {
                    // ex.printStackTrace();
                }
            }
        }
        if(handlers.size() == 0) { requestContext.complete(); }
    }

    /**
     * 此處重新建立各個 Handler 新實例的用意在於執行緒安全，
     * 雖然 Executor 層級已經是新實例，但如果 Handler 層級沒有採用新實例，
     * 而是一律採用使用者建立的實例時，會發生 AsyncContext 執行緒不安全的情況，
     * 間接發生 complete 重複的錯誤，這將會影響整體架構的穩定性
     */
    private ArrayList<RequestHandler> getHandlerChain() {
        ArrayList<RequestHandler> handlers = WebAppServicePoolBuilder.build().prototype(); // 物件範本
        ArrayList<RequestHandler> runHandlers = new ArrayList<>();
        // 建立新實例
        for (RequestHandler handler_raw : handlers) {
            try {
                RequestHandler handler = handler_raw.getClass().newInstance();
                runHandlers.add(handler);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 重新設定新實例的責任鏈關係
        if(runHandlers.size() > 0) {
            for(int i = 0, len = runHandlers.size(); i < len; i++) {
                RequestHandler handler = handlers.get(i);
                if(null == handler.getNextHandler()) {
                    if (i < (len - 1)) {
                        RequestHandler nextHandler = handlers.get(i + 1);
                        handler.setNextHandler(nextHandler);
                    }
                }
            }
        }
        return runHandlers;
    }

}
