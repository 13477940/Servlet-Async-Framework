package app.handler;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import framework.observer.Handler;
import framework.observer.Message;
import framework.web.context.AsyncActionContext;
import framework.web.handler.RequestHandler;

import java.io.File;

/**
 * 檔案下載範例
 */
public class FileHandler extends RequestHandler {

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
        if(asyncActionContext.isFileAction()) return false;
        return "file".equalsIgnoreCase(asyncActionContext.getParameters().get("act"));
    }

    private void processRequest() {
        String path = requestContext.getParameters().get("path");
        JsonObject resObj = getDirContent(path);
        try {
            // 如果是檔案則直接下載，若是資料夾則顯示其檔案列表
            if ("path_is_file".equals(resObj.get("status").getAsString())) {
                File file = new File(path);
                requestContext.outputFileToResponse(path, file.getName(), null, false, new Handler(){
                    @Override
                    public void handleMessage(Message m) {
                        super.handleMessage(m);
                        requestContext.complete();
                    }
                });
            } else {
                requestContext.printToResponse(resObj, new Handler(){
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
    private JsonObject getDirContent(String path) {
        String _path = "/";
        if(null != path && path.length() > 0) _path = path;
        File target = new File(_path);
        JsonObject obj = new JsonObject();
        if(target.isFile()) {
            obj.addProperty("status", "path_is_file");
            obj.addProperty("msg_zh", "檔案類型的路徑");
            return obj;
        }
        JsonArray arr = new JsonArray();
        String[] list = target.list();
        if(null != list && list.length > 0) {
            for(String str : list) {
                arr.add(str);
            }
        }
        obj.addProperty("status", "done");
        obj.add("content", arr);
        return obj;
    }

}
