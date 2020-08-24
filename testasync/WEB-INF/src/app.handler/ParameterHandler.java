package app.handler;

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
        return asyncActionContext.getParameters().size() > 0;
    }

    private void processRequest() {
        // 重複使用測試
        System.out.println(requestContext.getRequestTextContent());
        // JsonObject test = new Gson().fromJson(requestContext.getRequestTextContent(), JsonObject.class);
        // System.out.println(test.get("events").getAsJsonArray().get(0).getAsJsonObject().get("postback").getAsJsonObject().get("data").getAsString());
        // System.out.println(new Gson().toJson(test));
        // System.out.println(requestContext.getRequestTextContent());
        // System.out.println(requestContext.getRequestByteContent());
        // System.out.println(requestContext.getRequestByteContent());

        // process http request parameters
        JsonObject obj = new JsonObject();
        for(Map.Entry<String, String> entry : requestContext.getParameters().entrySet()) {
            obj.addProperty(entry.getKey(), entry.getValue());
        }
        // process http request headers
        // addHeaderValues(obj);
        requestContext.printToResponse(obj, new Handler(){
            @Override
            public void handleMessage(Message m) {
                super.handleMessage(m);
                requestContext.complete();
            }
        });
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
