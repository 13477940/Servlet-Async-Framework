package framework.context;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
import java.net.URLConnection;
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

    private Handler handler; // for invalid runnable

    private final String uploadProgressTag = "upload_progress"; // for Session

    /**
     * 初始化 RequestContext
     */
    public AsyncActionContext(AsyncContext asyncContext) {
        this.asyncContext = asyncContext;
        try {
            if (null == asyncContext) {
                System.err.println("AsyncContext 為空值，無法正常執行服務");
            } else {
                this.httpSession = ((HttpServletRequest) asyncContext.getRequest()).getSession();
                this.reqMethod = ((HttpServletRequest) asyncContext.getRequest()).getMethod();
                createInvalidRequestHandler(); // 建立預設的無效請求監聽器
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    public ArrayList<FileItem> getFiles() {
        return this.files;
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
     * 重置 Session 中的上傳進度值內容
     */
    public void resetUploadProgress() {
        httpSession.setAttribute(this.uploadProgressTag, "");
    }

    /**
     * 取得上傳進度值於 Session 中的 Key，與自定義名詞衝突時方便統一修改
     */
    public String getUploadProgressTag() {
        return this.uploadProgressTag;
    }

    /**
     * 取得 Session 中記錄的上傳進度值
     */
    public JSONObject getUploadProgress() {
        String load = httpSession.getAttribute(this.uploadProgressTag).toString();
        JSONObject obj;
        if(null == load || "null".equals(load) || load.length() == 0) {
            obj = new JSONObject();
        } else {
            obj = JSON.parseObject(load);
        }
        return obj;
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
    public void writeFile(FileItem fileItem, String path, String fileName) {
        if(null == fileItem) {
            System.err.println("必須設定一個可用的 FileItem 物件");
            return;
        }
        if(fileItem.isFormField()) {
            System.err.println(fileItem.getFieldName() + " 是個請求參數，必須是實體二進位檔案才能進行寫入檔案的動作");
            return;
        }
        if(null == path || path.length() == 0) {
            System.err.println("必須設定一個可用的資料夾路徑");
            return;
        }
        if(null == fileName || fileName.length() == 0) {
            System.err.println("必須設定一個可用的檔案名稱");
            return;
        }
        // 寫入檔案至該資料夾中
        File file = new File(path, fileName);
        try {
            fileItem.write(file);
        } catch(Exception e) {
            e.printStackTrace();
            System.err.println(String.valueOf(fileItem.getName()) + " 該上傳檔案於建立檔案時發生錯誤");
        }
    }

    /**
     * 列印字串至 response 緩存中
     */
    public void printToResponse(String content) {
        ServletOutputStream out;
        ByteArrayInputStream bis;
        try {
            // 初始化資料流
            out = asyncContext.getResponse().getOutputStream();
            bis = new ByteArrayInputStream(content.getBytes("UTF-8"));
            // 建立緩衝機制
            int bufferSize = 4096; // byte
            byte[] buffer = new byte[bufferSize];
            int len;
            try {
                // 寫出內容至前端串流
                while((len = bis.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                // 清空記憶體暫存內容
                bis.close();
                out.flush();
            } catch(Exception e) {
                // 唯一會出錯的機會只有使用者中斷下載過程時（不可抗拒）
                // 於發生例外時仍嘗試關閉 stream 回收資源
                try {
                    bis.close();
                } catch(Exception ex) {
                    // ex.printStackTrace();
                }
            }
        } catch(Exception e) {
            // e.printStackTrace();
        }
    }

    /**
     * 輸出檔案至瀏覽器端，isAttachment 影響到瀏覽器是否能預覽（true 時會被當作一般檔案來下載）
     */
    public void outputFileToResponse(String path, String fileName, String mimeType, boolean isAttachment) {
        if(null == path) {
            System.err.println("不能輸出無效的路徑檔案");
            return;
        }
        File file = new File(path);

        // 檢查 Internet Explorer，如果是 MS-IE 體系的話要另外處理才不會中文亂碼
        HttpServletRequest request = ((HttpServletRequest) asyncContext.getRequest());
        HttpServletResponse response = ((HttpServletResponse) asyncContext.getResponse());
        boolean isIE = false;
        String userAgent = request.getHeader("user-agent");
        if(userAgent.contains("Windows") && userAgent.contains("Edge")) isIE = true;
        if(userAgent.contains("Windows") && userAgent.contains("Trident")) isIE = true;

        // 解決中文檔案編碼
        StringBuilder sbd_encn = new StringBuilder();
        try {
            if(isIE) {
                sbd_encn.append( java.net.URLEncoder.encode( fileName, "UTF-8" ) );
            } else {
                String proto = String.valueOf(request.getProtocol()).trim().toLowerCase();
                // 於 HTTP 2.0 與 IE 體系統一採用 UTF-8
                if(proto.equals("http/2.0")) {
                    sbd_encn.append( java.net.URLEncoder.encode( fileName, "UTF-8" ) );
                } else {
                    sbd_encn.append( new String( fileName.getBytes("UTF-8"), "ISO8859_1" ) );
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }

        // 如果沒有取得檔案名稱
        if(sbd_encn.toString().length() == 0) sbd_encn.append(file.getName());
        String encodeFileName = sbd_encn.toString();

        // 本地檔案 MIME 解析處理
        String fileMIME = null;
        if(null == mimeType) {
            try {
                fileMIME = URLConnection.guessContentTypeFromName(file.getName());
                if (null == fileMIME) {
                    fileMIME = MimetypesFileTypeMap.getDefaultFileTypeMap().getContentType(file);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            fileMIME = mimeType;
        }

        // 如果是純檔案類型則採 attachment 下載
        if(null == fileMIME) {
            System.err.println("匯出檔案判斷類型時出錯");
            return;
        }
        response.setContentType(fileMIME+";charset=UTF-8");
        if(isAttachment) {
            response.setHeader("Content-Disposition", "attachment;filename=\"" + encodeFileName + "\"");
        } else {
            response.setHeader("Content-Disposition", "inline;filename=\"" + encodeFileName + "\"");
        }
        response.setHeader("Content-Length", String.valueOf(file.length()));

        // 輸出檔案給瀏覽器下載
        ServletOutputStream out = null;
        FileInputStream ins = null;
        // 初始化檢查點
        try {
            out = response.getOutputStream();
            ins = new FileInputStream(file);
        } catch(Exception e) {
            e.printStackTrace();
        }
        // 建立緩衝機制
        int bufferSize = 4096; // byte
        byte[] buffer = new byte[bufferSize];
        int len = -1;
        // 匯出過程檢查點
        try {
            // 寫出內容至前端串流
            while((len = ins.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            // 清空記憶體暫存內容
            out.flush();
            ins.close();
        } catch(Exception e) {
            // 唯一會出錯的機會只有使用者中斷下載過程時（不可抗拒）
            // e.printStackTrace();
            try {
                out.flush();
                ins.close();
            } catch (Exception ex) {
                // ex.printStackTrace();
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
        handler = new Handler() {
            @Override
            public void handleMessage(Message m) {
                super.handleMessage(m);
                JSONObject obj = new JSONObject();
                obj.put("error_code", "404");
                obj.put("status", "invalid_request");
                obj.put("msg_zht", "無效的請求");
                AsyncActionContext.this.printToResponse(obj.toJSONString());
                AsyncActionContext.this.complete();
            }
        };
    }

    /**
     * 自定義無效請求事件的監聽器
     */
    public void setInvalidRequestHandler(Handler handler) {
        this.handler = handler;
    }

    /**
     * 取得無效請求事件的監聽器實例
     */
    public Handler getInvalidRequestHandler() {
        return this.handler;
    }

}
