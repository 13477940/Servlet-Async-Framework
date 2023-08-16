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
import java.nio.file.Paths;

/**
 * modify: 230505
 * -
 * 此 Handler 會先處理具有副檔名格式的請求，
 * 所以要注意是否有後面的 handler 需要附帶副檔名的請求，
 * 並由此處寫入避開的判斷原則才能正常使用
 */
public class ResourceFileHandler extends RequestHandler {

    private AsyncActionContext requestContext = null;

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
        if(asyncActionContext.isFileAction()) return false;
        if(asyncActionContext.getUrlPath().contains("WEB-INF")) return false;
        return (null != asyncActionContext.getResourceExtension() && !asyncActionContext.getResourceExtension().isEmpty());
    }

    private void process_request() {
        String path = URLDecoder.decode(requestContext.getUrlPath(), StandardCharsets.UTF_8);
        {
            String dirSlash = new PathContext().get_file_separator();
            path = path.replaceAll("/", dirSlash); // cover dirSlash
        }
        if(null == static_value.project_dir){
            FileFinder finder = new FileFinder.Builder().build();
            static_value.project_dir = finder.find("WEB-INF").getParentFile(); // project-dir
        }
        {
            // path 本身已具有根斜線，只需接上 path 即可
            File file = Paths.get(static_value.project_dir.getPath() + path).toFile();
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
                // 在 HTML5 中，text/javascript 已被棄用，官方建議使用 application/javascript 代替
                if("js".equalsIgnoreCase(requestContext.getResourceExtension())) {
                    fileMIME = "application/javascript";
                }
                if("css".equalsIgnoreCase(requestContext.getResourceExtension())) {
                    fileMIME = "text/css";
                }
                // ts 檔案
                if ("ts".equalsIgnoreCase(requestContext.getResourceExtension())) {
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

    private static class static_value {
        public static File project_dir = null;
    }

}
