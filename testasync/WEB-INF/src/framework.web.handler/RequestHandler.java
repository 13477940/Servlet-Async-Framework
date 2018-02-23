package framework.web.handler;

import framework.web.context.AsyncActionContext;
import framework.observer.Message;

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
            Message m = requestContext.getInvalidRequestHandler().obtainMessage();
            m.sendToTarget();
        }
    }

    /**
     * 取得下一位 Handler
     */
    public RequestHandler getNextHandler() {
        return this.nextRequestHandler;
    }

}