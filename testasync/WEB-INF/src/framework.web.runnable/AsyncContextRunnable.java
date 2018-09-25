package framework.web.runnable;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import framework.web.context.AsyncActionContext;
import framework.web.executor.WebAppServiceExecutor;
import framework.web.session.context.UserContext;
import framework.web.session.pattern.UserMap;
import framework.web.session.service.SessionServiceStatic;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.apache.tomcat.util.http.fileupload.servlet.ServletRequestContext;

import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 每一個 HttpRequest 都會經過此處進行個別封裝，封裝後會得到 AsyncActionContext，
 * 並且作為核心應用單位進入使用者自訂義的 Request Handler 責任鏈中進行處理
 */
public class AsyncContextRunnable implements Runnable {

    private ServletContext servletContext;
    private ServletConfig servletConfig;
    private AsyncContext asyncContext;

    private AsyncActionContext requestContext;

    /**
     * 每個非同步請求實例派發給個別的 AsyncContextRunnable 隔離執行
     */
    private AsyncContextRunnable(ServletContext servletContext, ServletConfig servletConfig, AsyncContext asyncContext) {
        this.servletContext = servletContext;
        this.servletConfig = servletConfig;
        this.asyncContext = asyncContext;
        {
            this.requestContext = new AsyncActionContext.Builder()
                    .setServletContext(servletContext)
                    .setServletConfig(servletConfig)
                    .setAsyncContext(asyncContext)
                    .build();
            checkSessionLoginInfo(requestContext.getHttpSession());
        }
    }

    @Override
    public void run() {
        processRequest();
    }

    // 前端請求處理起始點
    private void processRequest() {
        String contentType = String.valueOf(asyncContext.getRequest().getContentType());
        if(contentType.contains("multipart/form-data")) {
            parseFormData();
        } else {
            parseParams();
        }
    }

    // 請求封裝處理完成，由此開始自定義的 Server 服務內容，並統一使用 requestContext 格式
    private void webAppStartup(HashMap<String, String> params, ArrayList<FileItem> files) {
        requestContext.setParameters(params);
        if(null == files || files.size() == 0) {
            requestContext.setIsFileAction(false); // 不具有檔案上傳請求
            requestContext.setFiles(null);
        } else {
            requestContext.setIsFileAction(true); // 具有檔案上傳請求
            requestContext.setFiles(files);
        }
        // 每個 WebAppServiceExecutor 獨立處理完一個 AsyncActionContext 的 Task 內容
        WebAppServiceExecutor executor = new WebAppServiceExecutor(requestContext);
        executor.startup();
    }

    // TODO 僅剩此處為同步狀態
    // 採用檔案處理方式解析 form-data 資料內容
    // 由於 Session 處理上傳進度值會影響伺服器效率，僅建議由前端處理上傳進度即可
    // jQuery 文件上传进度提示：https://segmentfault.com/a/1190000008791342
    private void parseFormData() {
        DiskFileItemFactory dfac = new DiskFileItemFactory();
        {
            // Sets the default charset for use when no explicit charset parameter is provided by the sender.
            dfac.setDefaultCharset(StandardCharsets.UTF_8.name());
            // Sets the size threshold beyond which files are written directly to disk.(bytes), default: 10k
            dfac.setSizeThreshold(DiskFileItemFactory.DEFAULT_SIZE_THRESHOLD);
            // Sets the directory used to temporarily store files that are larger than the configured size threshold.
            dfac.setRepository(new File(servletContext.getAttribute(ServletContext.TEMPDIR).toString()));
        }
        ServletFileUpload upload = new ServletFileUpload(dfac);

        // 上傳表單列表內容處理
        List<FileItem> items;
        try {
            items = upload.parseRequest(new ServletRequestContext((HttpServletRequest) asyncContext.getRequest()));
            createFileTable(items);
        } catch (Exception e) {
            e.printStackTrace();
            asyncContext.complete();
        }
    }

    // form-data 內容處理
    private void createFileTable(List<FileItem> items) {
        JSONObject obj_keys = new JSONObject();
        HashMap<String, String> params = new HashMap<>();
        ArrayList<FileItem> files = new ArrayList<>();
        for (FileItem fi : items) {
            if (fi.isFormField()) {
                // 表單資料：字串內容
                String key = fi.getFieldName();
                String value = "";
                try {
                    value = readFormDataTextContent(fi.getInputStream());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                {
                    // 單一 parameter key 具有多個參數時（多參數值藉由 JSONArray 字串形式儲存）
                    if(obj_keys.containsKey(key)) {
                        obj_keys.getJSONArray(key).add(value);
                    } else {
                        obj_keys.put(key, new JSONArray().add(value));
                    }
                }
                if(params.containsKey(key)) {
                    params.put(key, obj_keys.getJSONArray(key).toJSONString());
                } else {
                    params.put(key, value);
                }
            } else {
                // 實體檔案：二進位檔案內容
                files.add(fi);
            }
        }
        if(files.size() == 0) {
            webAppStartup(params, null);
        } else {
            webAppStartup(params, files);
        }
    }

    // 因為取得 form-data 的內容會造成中文亂碼，所以採用 UTF-8 的解決方式取值
    private String readFormDataTextContent(InputStream ins) {
        BufferedInputStream bis = new BufferedInputStream(ins);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        String res = null;
        try {
            int result = bis.read();
            while (result != -1) {
                buf.write((byte) result);
                result = bis.read();
            }
            {
                bis.close();
                buf.flush();
                res = buf.toString(StandardCharsets.UTF_8);
                buf.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    private void parseParams() {
        HashMap<String, String> params = new HashMap<>();
        for (Map.Entry<String, String[]> entry : asyncContext.getRequest().getParameterMap().entrySet()) {
            try {
                String key = entry.getKey();
                String[] values = entry.getValue();
                // if single key has more than one value, they will be add in JSONArray String.
                if (values.length > 1) {
                    JSONArray arr = new JSONArray();
                    Collections.addAll(arr, values);
                    params.put(key, arr.toJSONString());
                } else {
                    String value = values[0];
                    params.put(key, value);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        webAppStartup(params, null);
    }

    // 檢查使用者登入資訊是否已存在於 Session
    private void checkSessionLoginInfo(HttpSession session) {
        try {
            UserMap map = SessionServiceStatic.getInstance().getUserMap();
            if (null != map) {
                if(map.prototype().containsKey(session.getId())) return;
                UserContext userContext = SessionServiceStatic.getInstance().getUserContext(session);
                if (null != userContext) {
                    SessionServiceStatic.getInstance().setUserContext(session, userContext);
                    SessionServiceStatic.getInstance().addUser(session, userContext);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class Builder {

        private ServletContext servletContext;
        private ServletConfig servletConfig;
        private AsyncContext asyncContext;

        public AsyncContextRunnable.Builder setServletContext(ServletContext servletContext) {
            this.servletContext = servletContext;
            return this;
        }

        public AsyncContextRunnable.Builder setServletConfig(ServletConfig servletConfig) {
            this.servletConfig = servletConfig;
            return this;
        }

        public AsyncContextRunnable.Builder setAsyncContext(AsyncContext asyncContext) {
            this.asyncContext = asyncContext;
            return this;
        }

        public AsyncContextRunnable build() {
            return new AsyncContextRunnable(this.servletContext, this.servletConfig, this.asyncContext);
        }

    }

}
