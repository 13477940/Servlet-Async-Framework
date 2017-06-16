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
        ArrayList<RequestHandler> handlers = WebAppServicePoolBuilder.build().prototype();
        for(int i = 0, len = handlers.size(); i < len; i++) {
            RequestHandler handler = handlers.get(i);
            if(i < (len - 1)) {
                RequestHandler nextHandler = handlers.get(i+1);
                handler.setNextHandler(nextHandler);
            }
        }
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

}
