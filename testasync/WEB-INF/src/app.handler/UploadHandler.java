package app.handler;

import com.alibaba.fastjson.JSONObject;
import framework.observer.Handler;
import framework.observer.Message;
import framework.setting.AppSetting;
import framework.web.context.AsyncActionContext;
import framework.web.handler.RequestHandler;
import framework.web.multipart.FileItem;
import framework.web.multipart.FileItemList;

public class UploadHandler extends RequestHandler {

    private AsyncActionContext requestContext;

    @Override
    public void startup(AsyncActionContext requestContext) {
        this.requestContext = requestContext;
        if(checkIsMyJob(requestContext)) {
            processRequest();
        } else {
            this.passToNext(requestContext);
        }
    }

    @Override
    protected boolean checkIsMyJob(AsyncActionContext requestContext) {
        return ( requestContext.isFileAction() );
    }

    private void processRequest() {
        String uploadPath = new AppSetting.Builder().build().getPathContext().getUploadDirPath();
        FileItemList files = requestContext.getFiles();
        int index = 0;
        for (FileItem fi : files.prototype()) {
            requestContext.writeFile(fi, uploadPath, String.valueOf(System.currentTimeMillis()), new Handler(){
                @Override
                public void handleMessage(Message m) {
                    super.handleMessage(m);
                }
            });
            if(index == files.size() - 1) {
                JSONObject obj = new JSONObject();
                obj.put("status", "upload_done");
                obj.put("msg_zht", "上傳成功");
                obj.put("params", requestContext.getParameters());
                requestContext.printToResponse(obj.toJSONString(), new Handler(){
                    @Override
                    public void handleMessage(Message m) {
                        super.handleMessage(m);
                        requestContext.complete();
                    }
                });
            } else {
                index++;
            }
        }
    }

}
