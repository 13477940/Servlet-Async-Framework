package framework.web.context;

import com.alibaba.fastjson.JSONObject;
import framework.observer.Bundle;
import framework.observer.Handler;
import framework.observer.Message;
import org.apache.tomcat.util.http.fileupload.FileItem;

import javax.activation.MimetypesFileTypeMap;
import javax.servlet.AsyncContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * HttpRequest 通過了 Controller Servlet 後統一採用此規格作為 Request Context
 * 藉此降低重複操作原生 HttpContext 的麻煩，簡化處理複雜度及程式碼離散度，
 * 當偵測到最終沒有任何一個責任鏈的節點進行請求處理時，
 * 會調用 Invalid Request Handler 進行無效請求回覆
 */
public class AsyncActionContext {

    private AsyncContext asyncContext;
    private HttpSession httpSession;
    private String reqMethod;
    private boolean isFileAction = false; // 該請求是否具有上傳檔案的需求
    private HashMap<String, String> params;
    private ArrayList<FileItem> files; // 需要被操作的檔案（已不包含文字表單內容）
    private String urlPath = null; // 儲存此次請求路徑（不包含 domain）
    private String resourceExten = null; // 請求路徑資源的副檔名，若不具有副檔名則回傳 null

    private Handler appExceptionHandler; // for public exception
    private Handler invalidRequestHandler; // for invalid request

    /**
     * 初始化 RequestContext
     */
    public AsyncActionContext(AsyncContext asyncContext) {
        this.asyncContext = asyncContext;
        if (null == asyncContext) {
            System.err.println("AsyncContext 為空值，無法正常執行服務");
        } else {
            try {
                this.httpSession = ((HttpServletRequest) asyncContext.getRequest()).getSession(true);
                this.reqMethod = ((HttpServletRequest) asyncContext.getRequest()).getMethod();
                // 初始化
                {
                    createAppExceptionHandler(); // 任一個請求在尚未完畢時出現例外錯誤時的監聽器
                    createInvalidRequestHandler(); // 請求沒有被任一個責任節點處理時的監聽器
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

    public HttpSession getHttpSession() {
        return this.httpSession;
    }

    public HashMap<String, String> getParameters() {
        return this.params;
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

    /**
     * 取得是否具有檔案上傳請求的狀態值
     */
    public boolean getIsFileAction() {
        return this.isFileAction;
    }

    public String getMethod() {
        return reqMethod;
    }

    public AsyncContext getAsyncContext() {
        return this.asyncContext;
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
     * 暫存資料夾路徑：TomcatAppFiles/[WebAppName]/temp
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
        // 寫入檔案至該資料夾中
        File file = new File(path, fileName);
        {
            try {
                fileItem.write(file);
                {
                    Bundle b = new Bundle();
                    b.putString("status", "done");
                    b.putString("msg_zht", "上傳檔案寫入成功");
                    Message m = handler.obtainMessage();
                    m.setData(b);
                    m.sendToTarget();
                }
            } catch (Exception e) {
                // e.printStackTrace();
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
        String charset = "UTF-8";
        // 設定字串輸出為何種 HTTP Content-Type（要注意這個和 Content-Encoding 不同）
        getHttpResponse().setHeader("content-type", "text/plain;charset=" + charset.toLowerCase());
        {
            try {
                // ServletOutputStream
                ServletOutputStream out = asyncContext.getResponse().getOutputStream();
                // 建立緩衝機制
                int bufferSize = 4096; // byte
                byte[] buffer = new byte[bufferSize];
                int len;
                {
                    // 資料流操作
                    try (ByteArrayInputStream bis = new ByteArrayInputStream(content.getBytes(charset))) {
                        while ((len = bis.read(buffer)) != -1) {
                            out.write(buffer, 0, len);
                        }
                        out.flush();
                        {
                            Bundle b = new Bundle();
                            b.putString("status", "done");
                            b.putString("msg_zht", "輸出字串內容成功");
                            Message m = handler.obtainMessage();
                            m.setData(b);
                            m.sendToTarget();
                        }
                    } catch (Exception e) {
                        // e.printStackTrace();
                        out.flush();
                        {
                            Bundle b = new Bundle();
                            b.putString("status", "fail");
                            b.putString("msg_zht", "輸出字串內容失敗");
                            b.put("exception", e);
                            Message m = handler.obtainMessage();
                            m.setData(b);
                            m.sendToTarget();
                        }
                    }
                }
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

        // 檢查 Internet Explorer，如果是 MS-IE 體系的話要另外處理才不會中文亂碼
        boolean isIE = false;
        {
            String userAgent = request.getHeader("user-agent");
            if(!(null == userAgent || userAgent.length() == 0)) {
                try {
                    if (userAgent.contains("Windows") && userAgent.contains("Edge")) isIE = true;
                    if (userAgent.contains("Windows") && userAgent.contains("Trident")) isIE = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // 解決中文檔案編碼
        StringBuilder sbd_encn = new StringBuilder();
        {
            try {
                if (isIE) {
                    sbd_encn.append(java.net.URLEncoder.encode(fileName, "UTF-8"));
                } else {
                    String proto = String.valueOf(request.getProtocol()).trim().toLowerCase();
                    // 於 HTTP 2.0 與 IE 體系統一採用 UTF-8
                    if (proto.equals("http/2.0")) {
                        sbd_encn.append(java.net.URLEncoder.encode(fileName, "UTF-8"));
                    } else {
                        sbd_encn.append(new String(fileName.getBytes("UTF-8"), "ISO8859_1"));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                sbd_encn.append("undefined");
            }
        }

        // 如果沒有取得檔案名稱
        if(sbd_encn.toString().length() == 0) sbd_encn.append(file.getName());
        String encodeFileName = sbd_encn.toString();

        // 本地檔案 MIME 解析處理
        String fileMIME = null;
        {
            if (null == mimeType) {
                try {
                    // fileMIME = URLConnection.guessContentTypeFromName(file.getName());
                    // JDK 7+
                    fileMIME = Files.probeContentType(Paths.get(file.getPath()));
                    // MimetypesFileTypeMap need javax.activation.jar
                    if (null == fileMIME) fileMIME = new MimetypesFileTypeMap().getContentType(file);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                fileMIME = mimeType;
            }

            // 檢查 MIME 是否判斷正確
            if (null == fileMIME) {
                System.err.println("匯出檔案判斷 MIME 類型時出錯");
                return;
            }
        }

        // 如果指定為 isAttachment 則會被當作一般的檔案下載方式；若為 false 則會看瀏覽器能不能預覽該檔案，例如：pdf, json, html 等
        {
            response.setContentType(fileMIME + ";charset=UTF-8");
            if (isAttachment) {
                response.setHeader("Content-Disposition", "attachment;filename=\"" + encodeFileName + "\"");
            } else {
                response.setHeader("Content-Disposition", "inline;filename=\"" + encodeFileName + "\"");
            }
            response.setHeader("Content-Length", String.valueOf(file.length()));
        }

        // 匯出檔案實作
        {
            try {
                ServletOutputStream out = response.getOutputStream();
                {
                    // 建立緩衝機制
                    int bufferSize = 4096; // byte
                    byte[] buffer = new byte[bufferSize];
                    int len = -1;
                    // 資料流操作
                    try (FileInputStream ins = new FileInputStream(file)) {
                        while ((len = ins.read(buffer)) != -1) {
                            out.write(buffer, 0, len);
                        }
                        out.flush();
                        {
                            // 成功下載完畢時
                            Bundle b = new Bundle();
                            b.putString("status", "done");
                            Message m = handler.obtainMessage();
                            m.setData(b);
                            m.sendToTarget();
                        }
                    } catch (Exception e) {
                        // e.printStackTrace();
                        out.flush();
                        {
                            // 未成功下載完時
                            Bundle b = new Bundle();
                            b.putString("status", "fail");
                            b.putString("msg", e.getMessage());
                            b.put("exception", e);
                            Message m = handler.obtainMessage();
                            m.setData(b);
                            m.sendToTarget();
                        }
                    }
                }
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
        {
            HttpServletRequest httpRequest = null;
            try {
                httpRequest = (HttpServletRequest) this.asyncContext.getRequest();
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                String url = httpRequest.getRequestURL().toString();
                String[] sHttp = url.split("://");
                String[] sLoca = sHttp[sHttp.length - 1].split("/");
                StringBuilder sURL = new StringBuilder();
                for(int i = 0, len = sLoca.length; i < len; i++) {
                    String tmp = sLoca[i];
                    if(i == 0) continue;
                    sURL.append(tmp);
                    if(i != sLoca.length - 1) sURL.append("/");
                }
                this.urlPath = sURL.toString();
            } catch (Exception e) {
                e.printStackTrace();
            }

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

}
