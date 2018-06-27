package framework.web.runnable;

import framework.setting.AppSetting;
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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 每一個 HttpRequest 都會經過此處進行個別封裝，封裝後會得到 AsyncActionContext，
 * 並且作為核心應用單位進入使用者自訂義的 Request Handler 責任鏈中進行處理
 */
public class AsyncContextRunnable implements Runnable {

    private AsyncContext asyncContext;
    private AsyncActionContext requestContext;

    /**
     * 每個非同步請求實例派發給個別的 AsyncContextRunnable 隔離執行
     */
    public AsyncContextRunnable(AsyncContext asyncContext) {
        this.asyncContext = asyncContext;
        this.requestContext = new AsyncActionContext(asyncContext);
        checkSessionLoginInfo(requestContext.getHttpSession());
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

    // 採用檔案處理方式解析 form-data 資料內容
    // 由於使用 Session 處理上傳進度值會影響伺服器效率，僅建議由前端處理上傳進度即可
    private void parseFormData() {
        // 上傳設定初始化
        String tempFilePath = new AppSetting.Builder().build().getPathContext().getTempDirPath();
        File tempFile = new File(tempFilePath);
        DiskFileItemFactory dfac = new DiskFileItemFactory();
        dfac.setSizeThreshold(0); // 容量多少的上傳檔案可以被暫存在記憶體中
        dfac.setRepository(tempFile);
        ServletFileUpload upload = new ServletFileUpload(dfac);

        // 上傳表單列表內容處理
        List<FileItem> items;
        try {
            items = upload.parseRequest(new ServletRequestContext((HttpServletRequest) asyncContext.getRequest()));
            createFileTable(items);
        } catch(Exception e) {
            e.printStackTrace();
            asyncContext.complete();
        }
    }

    // form-data 內容處理
    private void createFileTable(List<FileItem> items) {
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
                if(params.containsKey(key)) {
                    int index = 1; // 起始 key 數
                    while(true) {
                        String numKey = key+"_"+index;
                        if(params.containsKey(numKey)) { // 如果仍為重複 key 時
                            index++;
                        } else {
                            params.put(numKey, value);
                            break;
                        }
                    }
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

    // 因為取得 form-data 的內容會造成中文亂碼，所以採用 Unicode 的解決方式取值
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
            // 回收資源與建立回傳
            bis.close();
            buf.flush();
            res = buf.toString(StandardCharsets.UTF_8);
            buf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    // GET URL 傳值處理，需統一將回傳值由 URLDecoder 處理，避免空格及特殊符號值傳遞的問題
    // 要注意的是前端 AJAX 也要採用 URLEncoder 將傳值進行處理才能避免錯誤
    private void parseParams() {
        Charset charset = StandardCharsets.UTF_8;
        HashMap<String, String> params = new HashMap<>();
        for (Map.Entry<String, String[]> entry : asyncContext.getRequest().getParameterMap().entrySet()) {
            try {
                String key = entry.getKey();
                if("get".equals(requestContext.getMethod().toLowerCase())) {
                    key = java.net.URLDecoder.decode(entry.getKey(), charset);
                }
                String[] values = entry.getValue();
                if (values.length > 1) {
                    // 單一 key 具有多個參數時
                    int iCount = 0;
                    for(String value : values) {
                        String _key = key;
                        if(iCount > 0) _key = key + "_" + iCount;
                        String _value = value;
                        if("get".equals(requestContext.getMethod().toLowerCase())) {
                            _value = java.net.URLDecoder.decode(value, charset);
                        }
                        params.put(_key, _value);
                        iCount++;
                    }
                } else {
                    // 單個參數時
                    String value = values[0];
                    if("get".equals(requestContext.getMethod().toLowerCase())) {
                        value = java.net.URLDecoder.decode(values[0], charset);
                    }
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

}
