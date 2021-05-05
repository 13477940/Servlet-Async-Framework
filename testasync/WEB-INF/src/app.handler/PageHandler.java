package app.handler;

import framework.file.FileFinder;
import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;
import framework.setting.PathContext;
import framework.web.context.AsyncActionContext;
import framework.web.handler.RequestHandler;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;

/**
 * 路徑判斷與頁面檔回傳範例
 */
public class PageHandler extends RequestHandler {

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
        if(null != asyncActionContext.getResourceExtension()) return false; // 排除資源類請求
        // if json form body
        if(asyncActionContext.getHeaders().containsKey("content-type")) {
            if (asyncActionContext.getHeaders().get("content-type").contains("application/json")) return false;
        }
        if("page".equals(asyncActionContext.getParameters().get("page"))) return true;
        return asyncActionContext.getParameters().size() == 0;
    }

    private void processRequest() {
        String dirSlash = new PathContext(this.getClass(), "testasync").getDirSlash();
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
        File appDir = new FileFinder.Builder().build().find("WEB-INF").getParentFile();
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
            requestContext.getHttpResponse().sendError(HttpServletResponse.SC_NOT_FOUND);
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
