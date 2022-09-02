package app.handler;

import com.google.gson.JsonObject;
import framework.observer.Handler;
import framework.observer.Message;
import framework.setting.PathContext;
import framework.web.context.AsyncActionContext;
import framework.web.handler.RequestHandler;
import framework.web.multipart.FileItem;
import framework.web.multipart.FileItemList;

import java.io.File;
import java.util.Map;

/**
 * 請求檔案上傳範例
 */
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
        String uploadPath = new PathContext().get_webapp_upload_file_folder_path();
        {
            File upload_folder = new File(uploadPath);
            if(!upload_folder.exists()) {
                {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("status", "fail");
                    obj.addProperty("msg_zht", "錯誤的上傳檔案路徑："+upload_folder);
                    requestContext.printToResponse(obj, new Handler(){
                        @Override
                        public void handleMessage(Message m) {
                            super.handleMessage(m);
                            requestContext.complete();
                        }
                    });
                }
                // upload_folder.mkdirs(); // 如果不具有該資料夾時
            }
        }
        if(null != requestContext.getFiles()) {
            FileItemList files = requestContext.getFiles();
            for (FileItem fi : files.prototype()) {
                if (!fi.isFormField()) {
                    String upload_file_name = "upload_"+ System.currentTimeMillis();
                    requestContext.writeFile(fi, uploadPath, upload_file_name, new Handler() {
                        @Override
                        public void handleMessage(Message m) {
                            super.handleMessage(m);
                        }
                    });
                }
            }
        }
        {
            JsonObject obj = new JsonObject();
            obj.addProperty("status", "upload_done");
            obj.addProperty("type", "file_upload");
            JsonObject params = new JsonObject();
            {
                if(null != requestContext.getParameters()) {
                    for (Map.Entry<String, String> param : requestContext.getParameters().entrySet()) {
                        params.addProperty(param.getKey(), param.getValue());
                    }
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
