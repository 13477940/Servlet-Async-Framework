package app.handler;

import com.google.gson.JsonObject;
import framework.observer.Handler;
import framework.observer.Message;
import framework.setting.AppSetting;
import framework.web.context.AsyncActionContext;
import framework.web.handler.RequestHandler;
import framework.web.multipart.FileItem;
import framework.web.multipart.FileItemList;

import java.util.Map;

public class UploadHandler extends RequestHandler {

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
        return asyncActionContext.isFileAction();
    }

    private void processRequest() {
        String uploadPath = new AppSetting.Builder().build().getPathContext().getUploadDirPath();
        FileItemList files = requestContext.getFiles();
        for (FileItem fi : files.prototype()) {
            if (!fi.isFormField()) {
                requestContext.writeFile(fi, uploadPath, String.valueOf(System.currentTimeMillis()), new Handler() {
                    @Override
                    public void handleMessage(Message m) {
                        super.handleMessage(m);
                    }
                });
            }
        }
        {
            JsonObject obj = new JsonObject();
            obj.addProperty("status", "upload_done");
            obj.addProperty("type", "file_upload");
            JsonObject params = new JsonObject();
            {
                for(Map.Entry<String, String> param : requestContext.getParameters().entrySet()) {
                    params.addProperty(param.getKey(), param.getValue());
                }
            }
            obj.add("params", params);
            requestContext.printToResponse(obj, new Handler() {
                @Override
                public void handleMessage(Message m) {
                    super.handleMessage(m);
                    requestContext.complete();
                }
            });
        }
    }

}
