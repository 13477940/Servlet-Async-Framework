package app.handler;

import com.alibaba.fastjson.JSONObject;
import framework.context.AsyncActionContext;
import framework.handler.RequestHandler;
import framework.setting.WebAppSettingBuilder;
import org.apache.tomcat.util.http.fileupload.FileItem;

import java.util.ArrayList;

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
        return ( requestContext.getIsFileAction() );
    }

    private void processRequest() {
        String uploadPath = WebAppSettingBuilder.build().getPathContext().getUploadDirPath();
        ArrayList<FileItem> files = requestContext.getFiles();
        for (FileItem fi : files) {
            requestContext.writeFile(fi, uploadPath, String.valueOf(System.currentTimeMillis()));
        }
        JSONObject obj = new JSONObject();
        obj.put("status", "upload_done");
        obj.put("msg_zht", "上傳成功");
        obj.put("params", requestContext.getParameters());
        requestContext.printToResponse(obj.toJSONString());
        requestContext.complete();
    }

}
