package app.handler;

import framework.file.FileFinder;
import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;
import framework.setting.AppSetting;
import framework.web.context.AsyncActionContext;
import framework.web.handler.RequestHandler;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * ResourceFileHandler 會先處理所有具有副檔名格式的 URL，
 * 所以要注意之後的 handler 是否需要處理具有副檔名的 URL，
 * 若需要另外處理請由此處寫入避開的判斷原則才能正常傳遞請求。
 */
public class ResourceFileHandler extends RequestHandler {

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
        if(asyncActionContext.isFileAction()) return false; // 排除上傳檔案請求
        if(asyncActionContext.getUrlPath().contains("/WEB-INF")) return false; // 基本安全機制
        return (null != asyncActionContext.getResourceExtension() && asyncActionContext.getResourceExtension().length() > 0);
    }

    private void processRequest() {
        String dirSlash = new AppSetting.Builder().build().getDirSlash();
        String path = requestContext.getUrlPath();
        {
            String[] tmp = path.split("/");
            StringBuilder sbd = new StringBuilder();
            int i = 0;
            for(String str : tmp) {
                sbd.append(URLDecoder.decode(str, StandardCharsets.UTF_8));
                i++;
                if(i <= tmp.length - 1) sbd.append("/");
            }
            path = sbd.toString();
        }
        path = path.replaceAll("/", dirSlash); // cover dirSlash
        FileFinder finder = new FileFinder.Builder().build();
        File dir = finder.find("WEB-INF").getParentFile(); // project-dir
        {
            // path 本身已具有根斜線，只需接上 path 即可
            File file = new File(dir.getPath() + path);
            if (!file.exists()) {
                response404(new Handler() {
                    @Override
                    public void handleMessage(Message m) {
                        super.handleMessage(m);
                        requestContext.complete();
                    }
                });
                return;
            }
            String fileMIME = null;
            {
                if ("js".equals(requestContext.getResourceExtension())) fileMIME = "text/javascript";
                if ("css".equals(requestContext.getResourceExtension())) fileMIME = "text/css";
            }
            requestContext.outputFileToResponse(file, file.getName(), fileMIME, false, new Handler() {
                @Override
                public void handleMessage(Message m) {
                    super.handleMessage(m);
                    requestContext.complete();
                }
            });
        }
    }

    private void response404(Handler handler) {
        try {
            requestContext.getHttpResponse().sendError(HttpServletResponse.SC_NOT_FOUND);
            {
                Bundle b = new Bundle();
                b.putString("status", "done");
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
