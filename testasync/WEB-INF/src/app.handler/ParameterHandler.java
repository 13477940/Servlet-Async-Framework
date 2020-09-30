package app.handler;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import framework.observer.Handler;
import framework.observer.Message;
import framework.web.context.AsyncActionContext;
import framework.web.handler.RequestHandler;

import java.util.Iterator;
import java.util.Map;

/**
 * 請求參數取得範例
 */
public class ParameterHandler extends RequestHandler {

    private AsyncActionContext requestContext;

    @Override
    public void startup(AsyncActionContext asyncActionContext) {
        if(checkIsMyJob(asyncActionContext)) {
            this.requestContext = asyncActionContext;
            processRequest();
        } else {
            this.passToNext(asyncActionContext);
        }
    }

    @Override
    protected boolean checkIsMyJob(AsyncActionContext asyncActionContext) {
        if("session".equals(asyncActionContext.getParameters().get("act"))) return false;
        // proc json
        if(asyncActionContext.getHeaders().containsKey("content-type")) {
            if (asyncActionContext.getHeaders().get("content-type").contains("application/json")) return true;
        }
        return asyncActionContext.getParameters().size() > 0;
    }

    private void processRequest() {
        JsonObject obj = new JsonObject();
        addHeaderValues(obj);
        addParamValues(obj);
        // System.out.println(new Gson().toJson(obj));
        requestContext.printToResponse(obj, new Handler(){
            @Override
            public void handleMessage(Message m) {
                super.handleMessage(m);
                requestContext.complete();
            }
        });
    }

    private void addParamValues(JsonObject obj) {
        if(requestContext.getHeaders().containsKey("content-type")) {
            if (requestContext.getHeaders().get("content-type").contains("application/json")) {
                // you can parse url and json content both
                JsonObject params = new JsonObject();
                for(Map.Entry<String, String> entry : requestContext.getParameters().entrySet()) {
                    params.addProperty(entry.getKey(), entry.getValue());
                }
                {
                    JsonObject parseObj = new Gson().fromJson(requestContext.getRequestTextContent(), JsonObject.class);
                    for(Map.Entry<String, JsonElement> entry : parseObj.entrySet()) {
                        params.add(entry.getKey(), entry.getValue());
                    }
                }
                obj.add("params", params);
            } else {
                JsonObject params = new JsonObject();
                for(Map.Entry<String, String> entry : requestContext.getParameters().entrySet()) {
                    params.addProperty(entry.getKey(), entry.getValue());
                }
                obj.add("params", params);
            }
        } else {
            JsonObject params = new JsonObject();
            for(Map.Entry<String, String> entry : requestContext.getParameters().entrySet()) {
                params.addProperty(entry.getKey(), entry.getValue());
            }
            obj.add("params", params);
        }
    }

    private void addHeaderValues(JsonObject obj) {
        JsonObject headers = new JsonObject();
        Iterator<String> it = requestContext.getHttpRequest().getHeaderNames().asIterator();
        while (it.hasNext()) {
            String key = it.next();
            String value = requestContext.getHttpRequest().getHeader(key);
            headers.addProperty(key, value);
        }
        obj.add("headers", headers);
    }

}
