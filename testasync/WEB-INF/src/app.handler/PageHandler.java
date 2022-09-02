package app.handler;

import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;
import framework.setting.AppSetting;
import framework.setting.PathContext;
import framework.web.context.AsyncActionContext;
import framework.web.handler.RequestHandler;

import javax.servlet.http.HttpServletResponse;
import java.io.File;

/**
 * 建立一個前端頁面檔案回傳的處理機制 RequestHandler，
 * 應注重系統頁面隱私性及路徑存取機制是否安全
 */
public class PageHandler extends RequestHandler {

    private AsyncActionContext requestContext;

    @Override
    public void startup(AsyncActionContext asyncActionContext) {
        if(checkIsMyJob(asyncActionContext)) {
            this.requestContext = asyncActionContext;
            process_request();
        } else {
            this.passToNext(asyncActionContext);
        }
    }

    @Override
    protected boolean checkIsMyJob(AsyncActionContext asyncActionContext) {
        if(asyncActionContext.isFileAction()) return false; // 排除 post multipart upload
        if(null != asyncActionContext.getResourceExtension()) return false; // 排除資源類請求
        // 排除對於 "content-type: application/json" 的請求處理
        if(asyncActionContext.getHeaders().containsKey("content-type")) {
            if (asyncActionContext.getHeaders().get("content-type").contains("application/json")) return false;
        }
        if("page".equalsIgnoreCase(asyncActionContext.getParameters().get("act"))) return true;
        return ( asyncActionContext.getParameters().size() == 0 );
    }

    private void process_request() {
        String dirSlash = new PathContext().get_file_separator();
        switch (requestContext.getUrlPath()) {
            case "/":
            case "/index": {
                outputPage(dirSlash + "index.html");
            } break;
            default: {
                response404();
            } break;
        }
    }

    // 指定檔案路徑後輸出該頁面檔案，如果仍然不具有檔案則回傳 404
    private void outputPage(String path) {
        outputPage(path, new Handler(){
            @Override
            public void handleMessage(Message m) {
                super.handleMessage(m);
                requestContext.complete();
            }
        });
    }
    private void outputPage(String path, Handler handler) {
        File file = getPageFile(path);
        if(null != file && file.exists()) {
            requestContext.outputFileToResponse(file, file.getName(), "text/html", false, handler);
        } else {
            response404(handler);
        }
    }

    private File getPageFile(String path) {
        // File appDir = new FileFinder.Builder().build().find("WEB-INF").getParentFile();
        File appDir = new AppSetting.Builder().build().getWebAppDir(); // test api
        File res = new File(appDir.getPath() + path);
        if(!res.exists()) return null;
        return res;
    }

    private void response404() {
        response404(new Handler(){
            @Override
            public void handleMessage(Message m) {
                super.handleMessage(m);
                requestContext.complete();
            }
        });
    }

    private void response404(Handler handler) {
        try {
            // 前端顯示 404 page
            requestContext.getHttpResponse().sendError(HttpServletResponse.SC_NOT_FOUND);
            // 後台 Handler 接收到的內容
            if(null != handler) {
                Bundle b = new Bundle();
                b.putString("status", "fail");
                b.putString("error_code", "404");
                b.putString("msg_zht", "沒有正確的指定頁面網址");
                Message m = handler.obtainMessage();
                m.setData(b);
                m.sendToTarget();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
