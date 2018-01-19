package app.handler;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import framework.observer.Handler;
import framework.observer.Message;
import framework.web.context.AsyncActionContext;
import framework.web.handler.RequestHandler;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;

public class FileHandler extends RequestHandler {

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
        return ( params.containsKey("act") && "file".equals(params.get("act")) );
    }

    private void processRequest() {
        String path = requestContext.getParameters().get("path");
        JSONObject res = getDirContent(path);
        try {
            if ("path_is_file".equals(res.getString("status"))) {
                File file = new File(path);
                requestContext.outputFileToResponse(path, file.getName(), null, false, new Handler(){
                    @Override
                    public void handleMessage(Message m) {
                        super.handleMessage(m);
                        requestContext.complete();
                    }
                });
            } else {
                requestContext.printToResponse(res.toJSONString(), new Handler(){
                    @Override
                    public void handleMessage(Message m) {
                        super.handleMessage(m);
                        requestContext.complete();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            requestContext.complete();
        }
    }

    // 取出該路徑下所有資料
    private JSONObject getDirContent(String path) {
        String _path = "/";
        if(null != path && path.length() > 0) _path = path;
        File target = new File(_path);
        JSONObject obj = new JSONObject();
        if(target.isFile()) {
            obj.put("status", "path_is_file");
            obj.put("msg_zh", "檔案類型的路徑");
            return obj;
        }
        JSONArray arr = new JSONArray();
        String[] list = target.list();
        if(null != list && list.length > 0) arr.addAll(Arrays.asList(list));
        obj.put("status", "done");
        obj.put("content", arr);
        return obj;
    }

}
