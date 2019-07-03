package app.handler;

import com.alibaba.fastjson.JSONObject;
import framework.observer.Handler;
import framework.observer.Message;
import framework.web.context.AsyncActionContext;
import framework.web.handler.RequestHandler;

public class PageHandler extends RequestHandler {

    private AsyncActionContext requestContext;

    @Override
    public void startup(AsyncActionContext requestContext) {
        this.requestContext = requestContext;
        if(checkIsMyJob(requestContext)) {
            processRequest();
        } else {
            passToNext(requestContext);
        }
    }

    @Override
    protected boolean checkIsMyJob(AsyncActionContext asyncActionContext) {
        if(asyncActionContext.isFileAction()) return false;
        if(null != asyncActionContext.getResourceExtension()) return false; // 排除資源類請求
        if("page".equals(asyncActionContext.getParameters().get("page"))) return true;
        return asyncActionContext.getParameters().size() == 0;
    }

    private void processRequest() {
        JSONObject obj = new JSONObject();
        obj.put("url", requestContext.getHttpRequest().getRequestURL().toString());
        obj.put("status", "page_request");
        obj.put("value", String.valueOf(requestContext.getParameters()));
        requestContext.printToResponse(obj.toJSONString(), new Handler(){
            @Override
            public void handleMessage(Message m) {
                super.handleMessage(m);
                requestContext.complete();
            }
        });
    }

}
