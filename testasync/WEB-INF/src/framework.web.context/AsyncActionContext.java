package framework.web.context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;
import framework.random.RandomServiceStatic;
import framework.setting.AppSetting;
import framework.text.TextFileWriter;
import framework.web.listener.AsyncWriteListener;
import framework.web.multipart.FileItem;
import framework.web.multipart.FileItemList;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.tika.Tika;

import java.io.*;
import java.lang.ref.WeakReference;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * 請求封裝層類別關係：
 * WebAppController > AsyncContextRunnable > AsyncActionContext
 *
 * 2020-04-16 修正 IE 與現代化瀏覽器下載 unicode 檔案名稱亂碼的問題
 * 2020-04-20 修正下載檔案名稱包含空格會被轉換為 + 符號的問題（因為 java 只有一種 URL Encode 模式）
 */
public class AsyncActionContext {

    private final ServletContext servletContext;
    private final ServletConfig servletConfig;
    private final AsyncContext asyncContext;

    private HttpSession httpSession;
    private String reqMethod;
    private boolean isFileAction = false; // 該請求是否具有上傳檔案的需求
    private HashMap<String, String> headers;
    private LinkedHashMap<String, String> params;
    private FileItemList files; // 已被暫存的檔案（不包含文字表單內容）

    private String urlPath = null;
    private String resourceExten = null; // 請求路徑資源的副檔名，若不具有副檔名則回傳 null

    private Handler appExceptionHandler; // for request all exception
    private Handler invalidRequestHandler; // for invalid request(no handler processed)

    private String asyncStatus = "onProcess"; // 該請求的 AsyncContext 狀態目前為何
    private boolean isComplete = false; // 這個 AsyncContext 是否已被 complete
    private boolean isOutput = false; // 限制每個 AsyncContext 只能輸出一次資料的機制

    private File temp_file_req_text = null; // 暫存的請求內容純文字檔
    private File temp_file_req_byte = null; // 暫存的請求內容二進位檔

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
                AsyncListener asyncListener = new WeakReference<AsyncListener>(
                    new AsyncListener() {

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

                    }
                ).get();
                this.asyncContext.addListener(asyncListener);
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

    public void setParameters(LinkedHashMap<String, String> params) {
        this.params = params;
    }

    public LinkedHashMap<String, String> getParameters() {
        return this.params;
    }

    /**
     * 回傳 HttpRequest Parameter Keys
     * 要注意 tomcat 環境中會依照使用者傳遞的參數順序，而 jetty 並不會依照使用者傳遞的順序
     * 如果需要驗證傳遞內容的功能，則應該採用 base64-url-safe 配合 JSON 格式儲存內容去溝通
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
        if( null == this.files || this.files.size() == 0 ) return null;
        HashMap<String, Integer> sort_map = new HashMap<>(); // 同 key 多檔案排序
        HashMap<String, FileItem> map = new HashMap<>();
        for( FileItem fileItem : this.files.prototype() ) {
            String key = fileItem.getFieldName();
            if( map.containsKey(key) ) {
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
     * 直接輸出 InputStream 內容，適用於二進位內容直接輸出，並作為檔案形式傳遞至前端
     * 於 safari, ie 要注意匯出檔案名稱不能過長，要不然會被瀏覽器重新命名
     */
    public void outputInputStream(InputStream inputStream, String fileName, String mimeType, boolean isAttachment, Handler handler) {
        if(checkIsOutput()) return;

        String encodeFileName = encodeOutputFileName(fileName);

        // 本地檔案 MIME 解析處理
        String fileMIME = null;
        {
            if (null == mimeType || mimeType.length() == 0) {
                try {
                    fileMIME = new Tika().detect(inputStream);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                fileMIME = mimeType;
            }
            // MIME types (IANA media types)
            // https://developer.mozilla.org/zh-TW/docs/Web/HTTP/Basics_of_HTTP/MIME_types
            if (null == fileMIME || fileMIME.length() == 0) {
                fileMIME = "application/octet-stream";
            }
        }

        // 如果指定為 isAttachment 為 true 則不管 MIME 是什麼都會被當作一般的檔案下載；
        // 若 isAttachment 為 false 則由瀏覽器自身決定能否瀏覽該檔案類型，例如：pdf, json, html 等
        // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition
        // 解決 Content-Disposition 跨瀏覽器編碼的問題：
        // https://blog.robotshell.org/2012/deal-with-http-header-encoding-for-file-download/
        HttpServletResponse response = ((HttpServletResponse) asyncContext.getResponse());
        {
            // ContentType
            response.setContentType( fileMIME + ";charset=" + StandardCharsets.UTF_8.name() );
            // Content-Disposition
            StringBuilder sbd = new StringBuilder();
            if (isAttachment) {
                sbd.append("attachment;filename=\"");
            } else {
                sbd.append("inline;filename=\"");
            }
            sbd.append(encodeFileName);
            sbd.append("\";");
            sbd.append("filename*=utf-8''"); // use for modern browser
            sbd.append(encodeFileName);
            response.setHeader("Content-Disposition", sbd.toString());
        }
        // 使用 WriteListener 非同步輸出
        {
            AsyncWriteListener asyncWriteListener = new AsyncWriteListener.Builder()
                    .setAsyncActionContext(this)
                    .setInputStream(inputStream)
                    .setHandler(handler)
                    .build();
            setOutputWriteListener(asyncWriteListener);
        }
    }

    /**
     * 輸出 Gson JsonObject 至 Response
     * 此方法會以 text/plain 格式回傳至前端
     */
    public void printToResponse(JsonObject jsonObject, Handler handler) {
        // printToResponse(new Gson().toJson(jsonObject), handler);
        // 221019 解決 html escaping string 會被轉換為 unicode 的問題
        printToResponse(new GsonBuilder().disableHtmlEscaping().create().toJson(jsonObject), handler);
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
                String tmpRespName = RandomServiceStatic.getInstance().getTimeHash(6);
                sbd.append(encodeOutputFileName(tmpRespName)).append(".txt");
                sbd.append("\"");
                sbd.append("filename*=utf-8''"); // use for modern browser
                sbd.append(encodeOutputFileName(tmpRespName)).append(".txt");
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
     * 此方法會以 application/json 格式回傳至前端
     */
    public void outputJSONToResponse(JsonObject obj, Handler handler) {
        if(checkIsOutput()) return;
        HttpServletResponse response = ((HttpServletResponse) asyncContext.getResponse());
        final String outputString = new Gson().toJson(obj);
        // 因為採用 byte 輸出，如果沒有 Response Header 容易在瀏覽器端發生錯誤
        {
            response.setContentType("application/json;charset="+StandardCharsets.UTF_8.name());
            StringBuilder sbd = new StringBuilder();
            {
                sbd.append("inline;filename=\"");
                String tmpRespName = RandomServiceStatic.getInstance().getTimeHash(6);
                sbd.append(encodeOutputFileName(tmpRespName)).append(".json");
                sbd.append("\"");
                sbd.append("filename*=utf-8''"); // use for modern browser
                sbd.append(encodeOutputFileName(tmpRespName)).append(".json");
            }
            response.setHeader("Content-Disposition", sbd.toString());
            response.setHeader("Content-Length", String.valueOf( outputString.getBytes(StandardCharsets.UTF_8).length ));
        }
        // 使用 WriteListener 非同步輸出
        {
            AsyncWriteListener asyncWriteListener = new AsyncWriteListener.Builder()
                    .setAsyncActionContext(this)
                    .setCharSequence(outputString)
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
        final File file = new File(path);
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
     * 於 safari, ie 要注意匯出檔案名稱不能過長，要不然會被瀏覽器重新命名
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
                    // fileMIME = Files.probeContentType(Paths.get(file.getPath()));
                    fileMIME = new Tika().detect(file);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                fileMIME = mimeType;
            }
            // MIME types (IANA media types)
            // https://developer.mozilla.org/zh-TW/docs/Web/HTTP/Basics_of_HTTP/MIME_types
            if (null == fileMIME || fileMIME.length() == 0) {
                fileMIME = "application/octet-stream";
            }
        }

        // 如果指定為 isAttachment 則不管 MIME 是什麼都會被當作一般的檔案下載；
        // 若 isAttachment 為 false 則會依照瀏覽器自身決定能不能瀏覽該檔案類型，例如：pdf, json, html 等
        // https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition
        // 解決 Content-Disposition 跨瀏覽器編碼的問題：
        // https://blog.robotshell.org/2012/deal-with-http-header-encoding-for-file-download/
        HttpServletResponse response = ((HttpServletResponse) asyncContext.getResponse());
        {
            // ContentType
            response.setContentType( fileMIME + ";charset=" + StandardCharsets.UTF_8.name() );
            // Content-Disposition
            StringBuilder sbd = new StringBuilder();
            if (isAttachment) {
                sbd.append("attachment;filename=\"");
            } else {
                sbd.append("inline;filename=\"");
            }
            sbd.append(encodeFileName);
            sbd.append("\";");
            sbd.append("filename*=utf-8''"); // use for modern browser
            sbd.append(encodeFileName);
            response.setHeader("Content-Disposition", sbd.toString());
            // Content-Length
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
     * 取消該次請求 Http Response Cache 狀態
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
                    JsonObject obj = new JsonObject();
                    obj.addProperty("error_code", "404");
                    obj.addProperty("status", "invalid_request");
                    obj.addProperty("msg_zht", "無效的請求");
                    AsyncActionContext.this.printToResponse(new Gson().toJson(obj), new Handler(){
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
                    JsonObject obj = new JsonObject();
                    obj.addProperty("status", m.getData().getString("status"));
                    obj.addProperty("error_code", "500"); // server_side
                    obj.addProperty("msg", m.getData().getString("msg"));
                    obj.addProperty("msg_zht", m.getData().getString("msg_zht"));
                    AsyncActionContext.this.printToResponse(new Gson().toJson(obj), new Handler(){
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
     * 如果根目錄會出現 webapp name 則代表 web.xml 的 <display-name></display-name> 不是正確的 webapp name
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
     *
     * #200806 修正因 stream 模式造成重複取值會為空值的問題
     */
    public String getRequestTextContent() {
        boolean is_disk_mode = false; // 是否先將資料寫入硬碟
        String res = null;
        if(is_disk_mode) {
            // 如果已建立過暫存檔案
            if(null != temp_file_req_text) {
                try {
                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(temp_file_req_text));
                    res = new String(bis.readAllBytes(), StandardCharsets.UTF_8);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return res;
            }
            // 第一次建立暫存檔案
            AppSetting appSetting = new AppSetting.Builder().build();
            {
                boolean file_create_status = false;
                temp_file_req_text = new File(appSetting.getPathContext().getTempDirPath()+"temp_txt_"+RandomServiceStatic.getInstance().getTimeHash(8));
                try {
                    file_create_status = temp_file_req_text.createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(file_create_status) temp_file_req_text.deleteOnExit(); // temp file
            }
            TextFileWriter textFileWriter = new TextFileWriter.Builder()
                    .setTargetFile(temp_file_req_text)
                    .setIsAppend(true)
                    .build();
            try {
                BufferedInputStream bis = new BufferedInputStream(asyncContext.getRequest().getInputStream());
                res = new String(bis.readAllBytes(), StandardCharsets.UTF_8);
                textFileWriter.write(res);
                textFileWriter.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                BufferedInputStream bis = new BufferedInputStream(asyncContext.getRequest().getInputStream());
                res = new String(bis.readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return res;
    }

    /**
     * 若是 Request 為純檔案傳輸（image/jpeg, image/png...），
     * 可由此方法將 InputStream 內容轉換為 File 的型態，
     * 這個檔案會被暫存至 Tomcat or Jetty 被關閉為止（deleteOnExit）
     *
     * #200806 修正因 stream 模式造成重複取值會為空值的問題
     */
    public File getRequestByteContent() {
        if(null != temp_file_req_byte) return temp_file_req_byte;
        File targetFile = null;
        AppSetting appSetting = new AppSetting.Builder().build();
        try {
            targetFile = new File(appSetting.getPathContext().getTempDirPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
        File nowFile = null;
        try {
            String prefixName = "upload_temp_" + System.currentTimeMillis() + "_";
            try {
                nowFile = new WeakReference<>( File.createTempFile(prefixName, null, targetFile) ).get();
            } catch (Exception e) {
                e.printStackTrace();
            }
            assert nowFile != null;
            nowFile.deleteOnExit();
            Files.copy(
                    asyncContext.getRequest().getInputStream(),
                    nowFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING
            );
            temp_file_req_byte = nowFile;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return temp_file_req_byte;
    }

    /**
     * 設定 AsyncContext Timeout，單位為 MilliSecond
     * 設定非同步處理時間上限，可以由此機制防止非同步請求陷入無止境的等待問題
     */
    public void setTimeout(long milliSecond) {
        this.asyncContext.setTimeout(milliSecond);
    }

    /**
     * 取得 Request 端 IP
     * https://stackoverflow.com/questions/18570747/servlet-get-client-public-ip
     */
    public String getRemoteIP() {
        HttpServletRequest req = (HttpServletRequest) asyncContext.getRequest();
        ArrayList<String> names = Collections.list(req.getHeaderNames());
        HashMap<String, String> headers = new HashMap<>();
        for(String key : names) {
            String value = req.getHeader(key);
            headers.put(key.toLowerCase(Locale.ENGLISH), value);
        }
        String ip = headers.get("x-forwarded-for");
        if(null == ip || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.get("proxy-client-ip");
        }
        if(null == ip || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = headers.get("wl-proxy-client-ip");
        }
        if(null == ip || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = req.getRemoteAddr();
        }
        return ip;
    }

    /**
     * 非同步架構下 ServletOutputStream 將綁定於 WriteListener 管理，
     * 為方便管理整體流程限制其 OutputStream 只能執行一次的機制，
     * 請將輸出資料以 JSON, Text 等格式單次輸出作為該次 HTTP 請求回傳內容
     * --
     * -- 1. 通常會看到此錯誤要注意是否 switch 條件未正確 break 而造成多個條件觸發
     * -- 2. 請檢查 if 等判斷式條件是否有重複執行的行為造成重複執行
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
     * 191112 修改為保留前端輸入網址的大小寫（確保大小寫敏感磁區運作邏輯）
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
        // 要注意 web.xml 中的 <display-name> 是否設定正確
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
                        String tmp = URLDecoder.decode(Objects.requireNonNullElse(sLoca[i], "").trim(), StandardCharsets.UTF_8);
                        if( i == 0 ) { continue; } // domain name
                        if( i == 1 && tmp.equals(servletContext.getServletContextName()) ) { continue; } // app name
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
     * 解決 output 下載檔案時 unicode 檔案名稱編碼
     *
     * https://tools.ietf.org/html/rfc3986
     * https://stackoverflow.com/questions/4737841/urlencoder-not-able-to-translate-space-character
     *
     * #201117 補充說明
     * 依照 rfc3986 java.net.URLEncoder.encode 將空格轉換為 "+" 是正確的，
     * 但 org.apache.catalina.util.URLEncoder.encode 的會轉換為 "%20" 較能正常使用，
     * 所以會採用 replaceAll("\\+", "%20") 達成其兩者間的一致性，
     * 而原本具有 "+" 符號的內容會被轉換為 %2B 所以並不會互相干擾
     */
    private String encodeOutputFileName(String fileName) {
        String encodeFileName = null;
        try {
            encodeFileName = java.net.URLEncoder
                    .encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return encodeFileName;
    }

    /**
     * 統一檢查與設定 AsyncWriteListener，以此管控每個請求只能輸出一次的機制，
     * 藉由此單一輸出機制簡化非同步與原本同步架構的 output 差異性
     */
    private void setOutputWriteListener(AsyncWriteListener asyncWriteListener) {
        isOutput = true;
        try {
            asyncContext.getResponse().getOutputStream().setWriteListener(asyncWriteListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 解析檔案名稱為檔名與副檔名
     * https://stackoverflow.com/questions/4545937/java-splitting-the-filename-into-a-base-and-extension
     */
    private String[] parseFileName(String str) {
        String[] tokens = str.split("\\.(?=[^.]+$)");
        for(int i = 0, len = tokens.length; i < len; i++) {
            tokens[i] = tokens[i].trim();
        }
        return tokens;
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
