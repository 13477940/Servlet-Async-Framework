package app.handler;

import com.alibaba.fastjson.JSONObject;
import framework.context.AsyncActionContext;
import framework.handler.RequestHandler;

import java.util.HashMap;

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
    protected boolean checkIsMyJob(AsyncActionContext requestContext) {
        HashMap<String, String> params = requestContext.getParameters();
        if(requestContext.getIsFileAction() && !params.containsKey("page")) return false;
        return ( null == params || params.size() == 0 || params.containsKey("page") );
    }

    private void processRequest() {
        JSONObject obj = new JSONObject();
        obj.put("url", requestContext.getHttpRequest().getRequestURL().toString());
        obj.put("status", "page_request");
        obj.put("value", String.valueOf(requestContext.getParameters()));
        requestContext.printToResponse(obj.toJSONString());
        requestContext.complete();
    }

}
