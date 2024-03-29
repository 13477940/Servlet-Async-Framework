package framework.web.executor;

import framework.observer.Bundle;
import framework.observer.Message;
import framework.web.context.AsyncActionContext;
import framework.web.handler.RequestHandler;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class WebAppServiceExecutor {

    private final AsyncActionContext requestContext;

    public WebAppServiceExecutor(AsyncActionContext asyncActionContext) {
        this.requestContext = asyncActionContext;
    }

    public void startup() {
        ArrayList<RequestHandler> handlers = getHandlerChain();
        // 尚未成功建立責任鏈時
        {
            if (handlers.isEmpty()) {
                Bundle b = new Bundle();
                b.putString("status", "fail");
                b.putString("msg", "not_exist_request_handler");
                b.putString("msg_zht", "尚未建立任何一個責任鏈節點 RequestHandler");
                Message m = requestContext.getAppExceptionHandler().obtainMessage();
                m.setData(b);
                m.sendToTarget();
                return;
            }
        }
        // 確認具有責任鏈，由第一節點進入開始執行
        try {
            handlers.get(0).startup(requestContext);
        } catch (Exception e) {
            // 當有 RequestHandler 開始處理時，但是在尚未到達 requestContext.complete() 結束時，
            // 程序中間出現任何未被擷取的 Exception 的情況下，就會呼叫此處的 Exception 處理，
            // 需要注意有可能是前端帶入值的問題，但後端沒有特別進行 Exception 處理也會到達此處
            e.printStackTrace();
            {
                Bundle b = new Bundle();
                b.putString("status", "fail");
                b.putString("msg", "server_side_exception_error");
                b.putString("msg_zht", "伺服器端例外錯誤，請檢查後台程式碼");
                Message m = requestContext.getAppExceptionHandler().obtainMessage();
                m.setData(b);
                m.sendToTarget();
            }
        }
    }

    /**
     * 此處重新建立各個 Handler 新實例(new Instance)的用意在於執行緒安全(Thread Safe)，
     * 雖然 Executor 層級已經是新實例，但如果 Handler 層級沒有採用新實例，
     * 而是一律採用初始化建立的實例時，會發生 AsyncContext 執行緒不安全的情況，
     * 使用者有可能使用到對方的請求責任鏈實例，進而發生短時間內可能重複 complete 的錯誤，
     * 藉由此方法修正該錯誤，加強套件功能的隔離性與穩健度
     */
    private ArrayList<RequestHandler> getHandlerChain() {
        ArrayList<RequestHandler> handlers = WebAppServicePoolStatic.getInstance().prototype(); // 物件範本
        ArrayList<RequestHandler> runHandlers = new ArrayList<>(); // 新實例容器（實際被運作的責任鏈節點實例）
        // 建立新實例，因為是由 ArrayList 實作所以經過 foreach 取出時順序是不變的
        for (RequestHandler rawHandler : handlers) {
            try {
                RequestHandler newHandler = rawHandler.getClass().getConstructor().newInstance();
                runHandlers.add( new WeakReference<>( newHandler ).get() );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // 重新建立新實例間的責任鏈關係（requestHandler 要知道是否還有下一位處理者）
        if(!runHandlers.isEmpty()) {
            // 首節點不具有前一個節點，所以可由 null 值判斷，接下來都將現有節點設定為上一個節點的 nextHandler
            RequestHandler prevHandler = null;
            for(RequestHandler runHandler : runHandlers) {
                if(null != prevHandler) prevHandler.setNextHandler(runHandler);
                prevHandler = runHandler;
            }
        }
        return new WeakReference<>( runHandlers ).get();
    }

}
