package framework.web.handler;

import framework.web.context.AsyncActionContext;

/**
 * 每一個 RequestHandler 通常代表一個種類的請求處理集合，
 * 藉由 checkIsMyJob() 實作檢查請求類型，
 * 並由 AsyncActionContext 內容實際進行處理，
 * 也可以在 RequestHandler 實例中實作子責任鏈，
 * 由子責任鏈處理更複雜的請求內容
 */
public abstract class RequestHandler {

    private RequestHandler nextRequestHandler;

    /**
     * Handler 處理事件起始點
     */
    public abstract void startup(AsyncActionContext requestContext);

    /**
     * Handler 確認是否為自身的工作
     */
    protected abstract boolean checkIsMyJob(AsyncActionContext requestContext);

    /**
     * 設定下一位 Handler
     */
    public void setNextHandler(RequestHandler handler) {
        this.nextRequestHandler = handler;
    }

    /**
     * 如果該項工作不屬於這位 Handler 轉交給下一個 Handler
     */
    protected void passToNext(AsyncActionContext requestContext) {
        if(null != nextRequestHandler) {
            this.nextRequestHandler.startup(requestContext);
        } else {
            // 如果是持續遞交到沒有下一個 handler 表示為無效的請求
            requestContext.getInvalidRequestHandler().obtainMessage().sendToTarget();
        }
    }

    /**
     * 取得下一位 Handler
     */
    public RequestHandler getNextHandler() {
        return this.nextRequestHandler;
    }

}
