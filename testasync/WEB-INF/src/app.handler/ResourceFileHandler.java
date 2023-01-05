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
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * 此 Handler 會先處理具有副檔名格式的請求，
 * 所以要注意是否有後面的 handler 需要附帶副檔名的請求，
 * 並由此處寫入避開的判斷原則才能正常使用
 * 修正 URL UTF-8 字串的問題
 */
public class ResourceFileHandler extends RequestHandler {

    private AsyncActionContext requestContext = null;

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
        if(asyncActionContext.getUrlPath().contains("WEB-INF")) return false;
        return (null != asyncActionContext.getResourceExtension() && asyncActionContext.getResourceExtension().length() > 0);
    }

    private void processRequest() {
        // 前端網址路徑轉換為檔案路徑
        String dirSlash = new PathContext().get_file_separator();
        String path = requestContext.getUrlPath();
        {
            String[] tmp = path.split("/");
            StringBuilder sbd = new StringBuilder();
            int i = 0;
            for(String str : tmp) {
                // fix for unicode path
                sbd.append(URLDecoder.decode(str, StandardCharsets.UTF_8));
                i++;
                if(i <= tmp.length - 1) sbd.append("/");
            }
            path = sbd.toString();
        }
        path = path.replaceAll("/", dirSlash); // cover dirSlash
        FileFinder finder = new FileFinder.Builder().build();
        // #210812 fix for windows IIS path
        File web_app_dir; // project-dir
        if( null != finder.find("WEB-INF") ) {
            web_app_dir = finder.find("WEB-INF").getParentFile();
        } else {
            web_app_dir = new File( new PathContext().get_webapp_project_folder_path() );
        }
        {
            // path 本身已具有根斜線，只需接上 path 即可
            File file = new File(web_app_dir.getPath() + path);
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
                // if you want protected typescript code or etc.
                if ("ts".equals(requestContext.getResourceExtension())) {
                    response404(new Handler(){
                        @Override
                        public void handleMessage(Message m) {
                            super.handleMessage(m);
                            requestContext.complete();
                        }
                    });
                    return;
                }
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
