package framework.web.context;

import com.alibaba.fastjson.JSONObject;
import framework.hash.HashServiceStatic;
import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;
import framework.random.RandomServiceStatic;
import framework.web.multipart.FileItem;
import framework.web.multipart.FileItemList;
import framework.web.niolistener.AsyncWriteListener;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;

/**
 * WebAppController > AsyncContextRunnable > AsyncActionContext
 */
public class AsyncActionContext {

    private ServletContext servletContext;
    private ServletConfig servletConfig;
    private AsyncContext asyncContext;

    private HttpSession httpSession;
    private String reqMethod;
    private boolean isFileAction = false; // 該請求是否具有上傳檔案的需求
    private HashMap<String, String> headers;
    private HashMap<String, String> params;
    private FileItemList files; // 已被暫存的檔案（不包含文字表單內容）
    private String urlPath = null;
    private String resourceExten = null; // 請求路徑資源的副檔名，若不具有副檔名則回傳 null

    private Handler appExceptionHandler; // for request all exception
    private Handler invalidRequestHandler; // for invalid request(no handler processed)

    private String asyncStatus = "onProcess"; // 該請求的 AsyncContext 狀態目前為何
    private boolean isComplete = false; // 這個 AsyncContext 是否已被 complete
    private boolean isOutput = false; // 限制每個 AsyncContext 只能輸出一次資料的機制

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
            // Servlet Async Listener
            {
                this.asyncContext.addListener(new AsyncListener() {

                    @Override
                    public void onStartAsync(AsyncEvent asyncEvent) { asyncStatus = "onStartAsync"; }

                    @Override
                    public void onComplete(AsyncEvent asyncEvent) {
                        asyncStatus = "onComplete";
                        isComplete = true;
                    }

                    @Override
                    public void onError(AsyncEvent asyncEvent) { asyncStatus = "onError"; }

                    @Override
                    public void onTimeout(AsyncEvent asyncEvent) { asyncStatus = "onTimeout"; }

                });
            }
            // 初始化 request context
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

    public void setHeaders(HashMap<String, String> headers) {
        this.headers = headers;
    }

    public HashMap<String, String> getHeaders() {
        return this.headers;
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

    public void setFiles(FileItemList files) {
        this.files = files;
    }

    /**
     * List 型態的 HTTP Request 上傳檔案內容
     */
    public FileItemList getFiles() {
        return this.files;
    }

    /**
     * Map 型態的 HTTP Request 上傳檔案內容
     */
    public HashMap<String, FileItem> getFilesMap() {
        if(null == this.files || this.files.size() == 0) return null;
        HashMap<String, Integer> sort_map = new HashMap<>(); // 同 key 多檔案排序
        HashMap<String, FileItem> map = new HashMap<>();
        for(FileItem fileItem : this.files.prototype()) {
            String key = fileItem.getFieldName();
            if(map.containsKey(key)) {
                Integer index = sort_map.getOrDefault(key, 0);
                sort_map.put(key, index);
                StringBuilder sbd = new StringBuilder();
                { sbd.append(key).append("_").append(index); }
                map.put(sbd.toString(), fileItem);
            } else {
                map.put(key, fileItem);
            }
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
     * 當一個 AsyncContext 被 complete 時，將會立刻回傳 response 給使用者
     * 若沒有經過 output 就 complete 可能會造成 response Content-Length 出錯
     */
    public void complete() {
        if(!isComplete) {
            this.isComplete = true;
            this.asyncStatus = "onComplete";
            this.asyncContext.complete();
        } else {
            try {
                throw new Exception("async context has been set complete status");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 回傳目前的 AsyncContext 是否已被 complete
     */
    public boolean isComplete() {
        return this.isComplete;
    }

    /**
     * 這將會取得目前 Servlet Async Status
     */
    public String getAsyncStatus() {
        return this.asyncStatus;
    }

    /**
     * 針對 framework.web.multipart.FileItem 格式進行檔案寫入
     * 屬於檔案類型的 form-data 寫入硬碟中儲存實作
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
            try ( InputStream tmpInputStream = fileItem.getInputStream() ) {
                // JDK 8+ NIO copy
                Files.copy(tmpInputStream, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
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
                    b.putString("msg_zht", fileItem.getName() + " 該上傳檔案於建立檔案時發生錯誤");
                    b.put("exception", e);
                    Message m = handler.obtainMessage();
                    m.setData(b);
                    m.sendToTarget();
                }
            }
        }
    }

    /**
     * 輸出純文本內容至 Response
     */
    public void printToResponse(String content, Handler handler) {
        if(checkIsOutput()) return;
        HttpServletResponse response = ((HttpServletResponse) asyncContext.getResponse());
        // 因為採用 byte 輸出，如果沒有 Response Header 容易在瀏覽器端發生錯誤
        {
            response.setContentType("text/plain;charset=" + StandardCharsets.UTF_8.name());
            StringBuilder sbd = new StringBuilder();
            {
                sbd.append("inline;filename=\"");
                String tmpRespName = HashServiceStatic.getInstance().stringToSHA256(RandomServiceStatic.getInstance().getTimeHash(6));
                sbd.append(encodeOutputFileName(tmpRespName)).append(".txt");
                sbd.append("\"");
            }
            response.setHeader("Content-Disposition", sbd.toString());
            response.setHeader("Content-Length", String.valueOf(content.getBytes(StandardCharsets.UTF_8).length));
        }
        // 使用 WriteListener 非同步輸出
        {
            AsyncWriteListener asyncWriteListener = new AsyncWriteListener.Builder()
                    .setAsyncActionContext(this)
                    .setCharSequence(content)
                    .setHandler(handler)
                    .build();
            setOutputWriteListener(asyncWriteListener);
        }
    }

    /**
     * 輸出 JSON 格式的字串至 Response
     */
    public void outputJSONToResponse(JSONObject obj, Handler handler) {
        if(checkIsOutput()) return;
        HttpServletResponse response = ((HttpServletResponse) asyncContext.getResponse());
        // 因為採用 byte 輸出，如果沒有 Response Header 容易在瀏覽器端發生錯誤
        {
            response.setContentType("application/json;charset="+StandardCharsets.UTF_8.name());
            StringBuilder sbd = new StringBuilder();
            {
                sbd.append("inline;filename=\"");
                String tmpRespName = HashServiceStatic.getInstance().stringToSHA256(RandomServiceStatic.getInstance().getTimeHash(6));
                sbd.append(encodeOutputFileName(tmpRespName)).append(".json");
                sbd.append("\"");
            }
            response.setHeader("Content-Disposition", sbd.toString());
            response.setHeader("Content-Length", String.valueOf(obj.toJSONString().getBytes(StandardCharsets.UTF_8).length));
        }
        // 使用 WriteListener 非同步輸出
        {
            AsyncWriteListener asyncWriteListener = new AsyncWriteListener.Builder()
                    .setAsyncActionContext(this)
                    .setCharSequence(obj.toJSONString())
                    .setHandler(handler)
                    .build();
            setOutputWriteListener(asyncWriteListener);
        }
    }

    /**
     * by FilePath
     * 輸出檔案至瀏覽器端；
     * isAttachment 影響到瀏覽器是否能預覽（true 時會被當作一般檔案來下載）
     */
    public void outputFileToResponse(String path, String fileName, String mimeType, boolean isAttachment, Handler handler) {
        if(checkIsOutput()) return;
        if(null == path || path.length() == 0) {
            {
                Bundle b = new Bundle();
                b.putString("status", "fail");
                b.putString("msg_zht", "指定的檔案是無效的");
                Message m = handler.obtainMessage();
                m.setData(b);
                m.sendToTarget();
            }
            return;
        }
        File file = new File(path);
        if(!file.exists()) {
            {
                Bundle b = new Bundle();
                b.putString("status", "fail");
                b.putString("msg_zht", "指定的檔案是無效的");
                Message m = handler.obtainMessage();
                m.setData(b);
                m.sendToTarget();
            }
            return;
        }
        outputFileToResponse(file, fileName, mimeType, isAttachment, handler);
    }

    /**
     * by File
     * 輸出檔案至瀏覽器端；
     * isAttachment 影響到瀏覽器是否能預覽（true 時會被當作一般檔案來下載）
     */
    public void outputFileToResponse(File file, String fileName, String mimeType, boolean isAttachment, Handler handler) {
        if(checkIsOutput()) return;

        // 檢查檔案是否正常
        if(null == file || file.length() == 0 || !file.exists()) {
            {
                Bundle b = new Bundle();
                b.putString("status", "fail");
                b.putString("msg_zht", "指定的檔案是無效的");
                Message m = handler.obtainMessage();
                m.setData(b);
                m.sendToTarget();
            }
            return;
        }

        // 如果沒有取得檔案名稱
        String encodeFileName = encodeOutputFileName(fileName);
        if(null == encodeFileName) encodeFileName = file.getName();

        // 本地檔案 MIME 解析處理
        String fileMIME = null;
        {
            if (null == mimeType || mimeType.length() == 0) {
                try {
                    fileMIME = Files.probeContentType(Paths.get(file.getPath()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                fileMIME = mimeType;
            }
            // MIME types (IANA media types)
            // https://developer.mozilla.org/zh-TW/docs/Web/HTTP/Basics_of_HTTP/MIME_types
            if (null == fileMIME) {
                fileMIME = "application/octet-stream";
            }
        }

        // 如果指定為 isAttachment 則不管 MIME 是什麼都會被當作一般的檔案下載；
        // 若 isAttachment 為 false 則會依照瀏覽器自身決定能不能瀏覽該檔案類型，例如：pdf, json, html 等
        // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition
        HttpServletResponse response = ((HttpServletResponse) asyncContext.getResponse());
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

        // 使用 WriteListener 非同步輸出
        {
            AsyncWriteListener asyncWriteListener = new AsyncWriteListener.Builder()
                    .setAsyncActionContext(this)
                    .setFile(file)
                    .setHandler(handler)
                    .build();
            setOutputWriteListener(asyncWriteListener);
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
     * Default Invalid Request Handler
     */
    private void createInvalidRequestHandler() {
        this.invalidRequestHandler = new Handler() {
            @Override
            public void handleMessage(Message m) {
                super.handleMessage(m);
                {
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
            }
        };
    }

    public Handler getInvalidRequestHandler() {
        return this.invalidRequestHandler;
    }

    public void setInvalidRequestHandler(Handler handler) {
        this.invalidRequestHandler = handler;
    }

    /**
     * Default Request Exception Handler
     */
    private void createAppExceptionHandler() {
        this.appExceptionHandler = new Handler(){
            @Override
            public void handleMessage(Message m) {
                super.handleMessage(m);
                {
                    JSONObject obj = new JSONObject();
                    obj.put("status", m.getData().getString("status"));
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
                }
            }
        };
    }

    public Handler getAppExceptionHandler() {
        return this.appExceptionHandler;
    }

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
     * 若是 Request 為 text/plain, application/json 等上傳格式，
     * 可由此方法將 InputStream 內容轉換為 String 的型態
     */
    public String getRequestTextContent() {
        String res = null;
        try {
            BufferedInputStream bis = new BufferedInputStream(asyncContext.getRequest().getInputStream());
            res = new String(bis.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    /**
     * 若是 Request 為純檔案傳輸（image/jpeg, image/png...），
     * 可由此方法將 InputStream 內容轉換為 File 的型態，
     * 這個檔案會被暫存至 Tomcat 被關閉為止（deleteOnExit）
     */
    public File getRequestByteContent() {
        File tomcat_temp = new File(servletContext.getAttribute(ServletContext.TEMPDIR).toString());
        String dirSlash = System.getProperty("file.separator");
        {
            String hostOS = System.getProperty("os.name");
            if (hostOS.toLowerCase().contains("windows")) {
                dirSlash = "\\\\";
            }
        }
        File tempFile;
        try {
            tempFile = File.createTempFile(RandomServiceStatic.getInstance().getTimeHash(3), null, new File(tomcat_temp.getPath() + dirSlash));
            tempFile.deleteOnExit();
            Files.copy(asyncContext.getRequest().getInputStream(), tempFile.toPath());
        } catch (Exception e) {
            e.printStackTrace();
            tempFile = null;
        }
        return tempFile;
    }

    /**
     * 設定 AsyncContext Timeout，單位為 MilliSecond
     * 設定非同步處理時間上限，可以由此機制防止非同步請求陷入無止境的等待問題
     */
    public void setTimeout(long milliSecond) {
        this.asyncContext.setTimeout(milliSecond);
    }

    /**
     * 非同步的 ServletOutputStream 因為綁定於 WriteListener 管理，
     * 所以只能有一次的輸出機會，請於請求完成後處理完所有需要輸出的內容合併輸出
     */
    private boolean checkIsOutput() {
        if(isOutput) {
            try {
                throw new Exception("Only have one output command for a WriteListener.");
            } catch (Exception e) {
                e.printStackTrace();
                AsyncActionContext.this.complete();
            }
        }
        return isOutput;
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
        // 檢查 ServletContextName 是否為空值
        {
            if(null == servletContext.getServletContextName()) {
                try {
                    throw new Exception("web.xml 需要設定 <display-name></display-name> 名稱，確保 AsyncActionContext 正常取得 AppName");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
        }
        // 請求路徑處理，去掉 ServletContextName
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
                        String tmp = URLDecoder.decode(Objects.requireNonNullElse(sLoca[i], "").trim().toLowerCase(), StandardCharsets.UTF_8);
                        if( i == 0 ) { continue; } // domain name
                        if( i == 1 && tmp.equals(servletContext.getServletContextName().toLowerCase()) ) { continue; } // app name
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

    /**
     * 解決下載檔案時 Unicode 檔案名稱編碼
     */
    private String encodeOutputFileName(String fileName) {
        HttpServletRequest request = ((HttpServletRequest) asyncContext.getRequest());
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
        return encodeFileName;
    }

    /**
     * 統一檢查與設定 AsyncWriteListener，以此管控每個請求只能輸出一次的機制
     */
    private void setOutputWriteListener(AsyncWriteListener asyncWriteListener) {
        isOutput = true;
        try {
            asyncContext.getResponse().getOutputStream().setWriteListener(asyncWriteListener);
        } catch (Exception e) {
            e.printStackTrace();
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
