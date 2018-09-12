package framework.web.context;

import com.alibaba.fastjson.JSONObject;
import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;
import framework.setting.AppSetting;
import framework.web.niolistener.AsyncWriteListener;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.IOUtils;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * HttpRequest 通過了 Controller Servlet 後統一採用此規格作為 Request Context
 * 藉此降低重複操作原生 HttpContext 的麻煩，簡化處理複雜度及程式碼離散度，
 * 當偵測到最終沒有任何一個責任鏈節點處理請求時，
 * 會調用 Invalid Request Handler 進行無效請求回覆，
 * 大部分的 Response 內容將由 AsyncActionContext 處理輸出
 */
public class AsyncActionContext {

    private ServletContext servletContext;
    private ServletConfig servletConfig;
    private AsyncContext asyncContext;

    private HttpSession httpSession;
    private String reqMethod;
    private boolean isFileAction = false; // 該請求是否具有上傳檔案的需求
    private HashMap<String, String> params;
    private ArrayList<FileItem> files; // 已被暫存的檔案（不包含文字表單內容）
    private String urlPath = null;
    private String resourceExten = null; // 請求路徑資源的副檔名，若不具有副檔名則回傳 null

    private Handler appExceptionHandler; // for request all exception
    private Handler invalidRequestHandler; // for invalid request(no handler processed)

    private AsyncActionContext(ServletContext servletContext, ServletConfig servletConfig, AsyncContext asyncContext) {
        this.servletContext = servletContext;
        this.servletConfig = servletConfig;
        this.asyncContext = asyncContext;
        if (null == asyncContext) {
            try {
                throw new Exception("AsyncContext 為空值，無法正常執行服務");
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                this.httpSession = ((HttpServletRequest) asyncContext.getRequest()).getSession(true);
                this.reqMethod = ((HttpServletRequest) asyncContext.getRequest()).getMethod();
                {
                    // 任一個請求在尚未完畢時出現例外錯誤時的監聽器
                    createAppExceptionHandler();
                    // 請求沒有被任一個責任節點處理時的監聽器
                    createInvalidRequestHandler();
                    // 處理 URL 路徑的解析（去除協定、域名及參數）
                    processUrlParse();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void setIsFileAction(boolean isFileAction) {
        this.isFileAction = isFileAction;
    }

    public void setParameters(HashMap<String, String> params) {
        this.params = params;
    }

    public HashMap<String, String> getParameters() {
        return this.params;
    }

    /**
     * 使用 ArrayList 回傳的原因在於保存使用者 key 輸入的順序
     */
    public ArrayList<String> getParameterKeys() {
        return Collections.list(this.asyncContext.getRequest().getParameterNames());
    }

    public HttpSession getHttpSession() {
        return this.httpSession;
    }

    public void setFiles(ArrayList<FileItem> files) {
        this.files = files;
    }

    /**
     * List 型態的 HTTP Request 上傳檔案內容
     */
    public ArrayList<FileItem> getFiles() {
        return this.files;
    }

    /**
     * Map 型態的 HTTP Request 上傳檔案內容
     */
    public HashMap<String, FileItem> getFilesMap() {
        if(null == this.files || this.files.size() == 0) return null;
        HashMap<String, FileItem> map = new HashMap<>();
        for(FileItem fileItem : this.files) {
            String key = fileItem.getFieldName(); // html form input name
            map.put(key, fileItem);
        }
        return map;
    }

    // 確認該請求是否需要檔案上傳
    public boolean isFileAction() {
        return this.isFileAction;
    }

    public String getMethod() {
        return reqMethod;
    }

    public AsyncContext getAsyncContext() {
        return this.asyncContext;
    }

    public ServletConfig getServletConfig() {
        return this.servletConfig;
    }

    public ServletContext getServletContext() {
        return this.servletContext;
    }

    public HttpServletRequest getHttpRequest() {
        HttpServletRequest httpRequest = null;
        try {
            httpRequest = (HttpServletRequest) this.asyncContext.getRequest();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return httpRequest;
    }

    public HttpServletResponse getHttpResponse() {
        HttpServletResponse httpResponse = null;
        try {
            httpResponse = (HttpServletResponse) this.asyncContext.getResponse();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return httpResponse;
    }

    /**
     * 於此次非同步請求處理完畢時呼叫，調用後會回傳 Response
     */
    public void complete() {
        this.asyncContext.complete();
    }

    /**
     * 屬於檔案類型的 form-data 寫入硬碟中儲存實作，
     * 如果於檔案上傳後沒有調用此方法，該檔案將會被放置在暫存資料夾中
     * 必須具有暫存資料夾路徑：TomcatAppFiles/[WebAppName]/temp
     */
    public void writeFile(FileItem fileItem, String path, String fileName, Handler handler) {
        if(null == fileItem) {
            Bundle b = new Bundle();
            b.putString("status", "fail");
            b.putString("msg_zht", "必須設定一個可用的 FileItem 物件");
            Message m = handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
            return;
        }
        if(fileItem.isFormField()) {
            Bundle b = new Bundle();
            b.putString("status", "fail");
            b.putString("msg_zht", fileItem.getFieldName() + " 是個請求參數，必須是二進位檔案格式才能進行寫入檔案的動作");
            Message m = handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
            return;
        }
        if(null == path || path.length() == 0) {
            Bundle b = new Bundle();
            b.putString("status", "fail");
            b.putString("msg_zht", "必須設定一個可用的資料夾路徑");
            Message m = handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
            return;
        }
        if(null == fileName || fileName.length() == 0) {
            Bundle b = new Bundle();
            b.putString("status", "fail");
            b.putString("msg_zht", "必須設定一個可用的檔案名稱");
            Message m = handler.obtainMessage();
            m.setData(b);
            m.sendToTarget();
            return;
        }
        {
            File test = new File(path);
            if(!test.exists()) {
                try {
                    throw new Exception(path + " 路徑並不存在");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
            if(!test.isDirectory()) {
                try {
                    throw new Exception(path + " 並不是資料夾");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
        }
        // 寫入檔案至該資料夾中
        File file = new File(path, fileName);
        {
            try {
                // JDK 8+ NIO copy
                InputStream tmpInputStream = fileItem.getInputStream();
                Files.copy(tmpInputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                IOUtils.closeQuietly(tmpInputStream);
                {
                    Bundle b = new Bundle();
                    b.putString("status", "done");
                    b.putString("msg_zht", "上傳檔案寫入成功");
                    Message m = handler.obtainMessage();
                    m.setData(b);
                    m.sendToTarget();
                }
            } catch (Exception e) {
                e.printStackTrace();
                {
                    Bundle b = new Bundle();
                    b.putString("status", "fail");
                    b.putString("msg_zht", String.valueOf(fileItem.getName()) + " 該上傳檔案於建立檔案時發生錯誤");
                    b.put("exception", e);
                    Message m = handler.obtainMessage();
                    m.setData(b);
                    m.sendToTarget();
                }
            }
        }
    }

    /**
     * 列印字串至 response 緩存中
     */
    public void printToResponse(String content, Handler handler) {
        // 輸出統一編碼
        String charset = StandardCharsets.UTF_8.name();
        // 設定字串輸出為何種 HTTP Content-Type（要注意這個和 Content-Encoding 不同）
        getHttpResponse().setHeader("content-type", "text/plain;charset=" + charset.toLowerCase());
        {
            AsyncWriteListener asyncWriteListener = new AsyncWriteListener.Builder()
                    .setAsyncContext(asyncContext)
                    .setCharSequence(content)
                    .setHandler(handler)
                    .build();
            try {
                asyncContext.getResponse().getOutputStream().setWriteListener(asyncWriteListener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * by FilePath
     * 輸出檔案至瀏覽器端；
     * isAttachment 影響到瀏覽器是否能預覽（true 時會被當作一般檔案來下載）
     */
    public void outputFileToResponse(String path, String fileName, String mimeType, boolean isAttachment, Handler handler) {
        if(null == path || path.length() == 0) {
            {
                Bundle b = new Bundle();
                b.putString("status", "fail");
                b.putString("msg_zht", "不能輸出無效的路徑檔案");
                Message m = handler.obtainMessage();
                m.setData(b);
                m.sendToTarget();
            }
            return;
        }
        File file = new File(path);
        outputFileToResponse(file, fileName, mimeType, isAttachment, handler);
    }

    /**
     * by File
     * 輸出檔案至瀏覽器端；
     * isAttachment 影響到瀏覽器是否能預覽（true 時會被當作一般檔案來下載）
     */
    public void outputFileToResponse(File file, String fileName, String mimeType, boolean isAttachment, Handler handler) {
        HttpServletRequest request = ((HttpServletRequest) asyncContext.getRequest());
        HttpServletResponse response = ((HttpServletResponse) asyncContext.getResponse());

        // 檢查瀏覽器是否為 Internet Explorer，如果是 MS-IE 體系的話要另外處理才不會中文亂碼
        boolean isIE = false;
        {
            String userAgent = request.getHeader("user-agent");
            if(null != userAgent && userAgent.length() > 0) {
                try {
                    if (userAgent.contains("Windows") && userAgent.contains("Edge")) isIE = true;
                    if (userAgent.contains("Windows") && userAgent.contains("Trident")) isIE = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // 解決下載檔案時 Unicode 檔案名稱編碼
        String encodeFileName = null;
        {
            try {
                if (isIE) {
                    encodeFileName = java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8);
                } else {
                    String protocol = String.valueOf(request.getProtocol()).trim().toLowerCase();
                    if (protocol.contains("http/2.0")) {
                        encodeFileName = java.net.URLEncoder.encode(fileName, StandardCharsets.UTF_8);
                    } else {
                        encodeFileName = new String(fileName.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 如果沒有取得檔案名稱
        if(null == encodeFileName) encodeFileName = file.getName();

        // 本地檔案 MIME 解析處理
        String fileMIME = null;
        {
            if (null == mimeType) {
                try {
                    // JDK 7+
                    fileMIME = Files.probeContentType(Paths.get(file.getPath()));
                    // javax.activation.jar
                    if (null == fileMIME) fileMIME = new MimetypesFileTypeMap().getContentType(file);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                fileMIME = mimeType;
            }

            // 檢查 MIME 是否判斷正確
            if (null == fileMIME) {
                try {
                    throw new Exception("MIME 為空值，匯出檔案判斷 MIME 類型時出錯");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
        }

        // 如果指定為 isAttachment 則不管 MIME 是什麼都會被當作一般的檔案下載；
        // 若為 false 則會依照瀏覽器自身決定能不能瀏覽該檔案類型，例如：pdf, json, html 等
        {
            response.setContentType( fileMIME + ";charset=" + StandardCharsets.UTF_8.name() );
            StringBuilder sbd = new StringBuilder();
            if (isAttachment) {
                sbd.append("attachment;filename=\"");
            } else {
                sbd.append("inline;filename=\"");
            }
            sbd.append(encodeFileName);
            sbd.append("\"");
            response.setHeader("Content-Disposition", sbd.toString());
            response.setHeader("Content-Length", String.valueOf(file.length()));
        }

        // 匯出檔案實作
        {
            AsyncWriteListener asyncWriteListener = new AsyncWriteListener.Builder()
                    .setAsyncContext(asyncContext)
                    .setFile(file)
                    .setHandler(handler)
                    .build();
            try {
                asyncContext.getResponse().getOutputStream().setWriteListener(asyncWriteListener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 取消該次請求 Http Cache 狀態
     */
    public void disableHttpCache() {
        try {
            HttpServletResponse resp = (HttpServletResponse) asyncContext.getResponse();
            resp.setHeader("Pragma", "no-cache");
            resp.setHeader("Cache-Control", "no-cache");
            resp.setDateHeader("Expires", 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 預設的無效請求事件實作
     */
    private void createInvalidRequestHandler() {
        this.invalidRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message m) {
                super.handleMessage(m);
                JSONObject obj = new JSONObject();
                obj.put("error_code", "404");
                obj.put("status", "invalid_request");
                obj.put("msg_zht", "無效的請求");
                AsyncActionContext.this.printToResponse(obj.toJSONString(), new Handler(){
                    @Override
                    public void handleMessage(Message m) {
                        super.handleMessage(m);
                        AsyncActionContext.this.complete();
                    }
                });
            }
        };
    }

    /**
     * 自定義無效請求事件的監聽器
     */
    public void setInvalidRequestHandler(Handler handler) {
        this.invalidRequestHandler = handler;
    }

    /**
     * 取得無效請求事件的監聽器實例
     */
    public Handler getInvalidRequestHandler() {
        return this.invalidRequestHandler;
    }

    /**
     * 預設的廣域 Exception 事件監聽器
     */
    private void createAppExceptionHandler() {
        this.appExceptionHandler = new Handler(){
            @Override
            public void handleMessage(Message m) {
                super.handleMessage(m);
                String status = m.getData().getString("status");
                if("fail".equals(status)) {
                    JSONObject obj = new JSONObject();
                    obj.put("status", status);
                    obj.put("error_code", "500"); // server_side
                    obj.put("msg_eng", m.getData().getString("msg_eng"));
                    obj.put("msg_zht", m.getData().getString("msg_zht"));
                    AsyncActionContext.this.printToResponse(obj.toJSONString(), new Handler(){
                        @Override
                        public void handleMessage(Message m) {
                            super.handleMessage(m);
                            AsyncActionContext.this.complete();
                        }
                    });
                } else {
                    // 基本上永遠不會到達此處
                    System.out.println("Public Exception Handler Message：");
                    System.out.println(m.getData().toString());
                    AsyncActionContext.this.complete();
                }
            }
        };
    }

    public Handler getAppExceptionHandler() {
        return this.appExceptionHandler;
    }

    /**
     * 自定義廣域 Exception Handler
     */
    public void setAppExceptionHandler(Handler handler) {
        this.appExceptionHandler = handler;
    }

    /**
     * 取得請求路徑（Path），此路徑資訊不包含 protocol, domain 及 parameters 等
     */
    public String getUrlPath() {
        return this.urlPath;
    }

    /**
     * 取得資源路徑的副檔名，例如 jquery.min.js 則會取得 js 字串內容，
     * 若不屬於資源路徑或不具有副檔名時則回傳 null
     */
    public String getResourceExtension() {
        return this.resourceExten;
    }

    /**
     * 處理 URL 路徑的解析（去除協定、域名及參數）
     */
    private void processUrlParse() {
        // HttpServletRequest 取得
        HttpServletRequest httpRequest = null;
        {
            try {
                httpRequest = (HttpServletRequest) this.asyncContext.getRequest();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(null == httpRequest) return;
        // 請求路徑字串處理
        {
            try {
                if(null == httpRequest.getRequestURL()) {
                    this.urlPath = null;
                } else {
                    String url = httpRequest.getRequestURL().toString();
                    String[] sHttp = url.split("://");
                    String[] sLoca = sHttp[sHttp.length - 1].split("/");
                    StringBuilder sURL = new StringBuilder();
                    sURL.append("/"); // root path
                    for (int i = 0, len = sLoca.length; i < len; i++) {
                        String tmp = sLoca[i];
                        if(i == 0) { continue; } // domain name
                        if(i == 1 && tmp.equals(new AppSetting.Builder().build().getAppName())) { continue; } // app name
                        // 網址處理後除了 domain name 及 app name 之外都會被當作 URI 字串內容
                        // example > http://localhost/webappname/a/index 處理後會變成 /a/index 字串
                        sURL.append(tmp);
                        if (i != sLoca.length - 1) sURL.append("/");
                    }
                    this.urlPath = sURL.toString();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if(null == this.urlPath) return;
        // 處理資源連結副檔名
        {
            try {
                String[] pathStrArr = this.urlPath.split("/");
                if(pathStrArr.length <= 1) return;
                String[] fileNameSplit = pathStrArr[pathStrArr.length - 1].split("\\.");
                if(fileNameSplit.length <= 1) return;
                this.resourceExten = fileNameSplit[fileNameSplit.length - 1];
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static class Builder {

        private ServletContext servletContext;
        private ServletConfig servletConfig;
        private AsyncContext asyncContext;

        public AsyncActionContext.Builder setServletContext(ServletContext servletContext) {
            this.servletContext = servletContext;
            return this;
        }

        public AsyncActionContext.Builder setServletConfig(ServletConfig servletConfig) {
            this.servletConfig = servletConfig;
            return this;
        }

        public AsyncActionContext.Builder setAsyncContext(AsyncContext asyncContext) {
            this.asyncContext = asyncContext;
            return this;
        }

        public AsyncActionContext build() {
            return new AsyncActionContext(this.servletContext, this.servletConfig, this.asyncContext);
        }

    }

}
