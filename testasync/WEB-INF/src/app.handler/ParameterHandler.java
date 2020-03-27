package app.handler;

import com.alibaba.fastjson.JSONObject;
import framework.observer.Handler;
import framework.observer.Message;
import framework.web.context.AsyncActionContext;
import framework.web.handler.RequestHandler;

import java.util.Map;

public class ParameterHandler extends RequestHandler {

    private AsyncActionContext requestContext = null;

    @Override
    public void startup(AsyncActionContext asyncActionContext) {
        if(checkIsMyJob(asyncActionContext)) {
            this.requestContext = asyncActionContext;
            processReuqest();
        } else {
            this.passToNext(asyncActionContext);
        }
    }

    @Override
    protected boolean checkIsMyJob(AsyncActionContext asyncActionContext) {
        return asyncActionContext.getParameters().size() > 0;
    }

    private void processReuqest() {
        JSONObject obj = new JSONObject();
        for(Map.Entry<String, String> entry : requestContext.getParameters().entrySet()) {
            obj.put(entry.getKey(), entry.getValue());
        }
        requestContext.printToResponse(obj.toJSONString(), new Handler(){
            @Override
            public void handleMessage(Message m) {
                super.handleMessage(m);
                requestContext.complete();
            }
        });
    }

}
