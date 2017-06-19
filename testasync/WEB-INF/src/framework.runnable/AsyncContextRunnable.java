package framework.runnable;

import framework.context.AsyncActionContext;
import framework.executor.WebAppServiceExecutor;
import framework.setting.WebAppSettingBuilder;
import org.apache.tomcat.util.http.fileupload.FileItem;
import org.apache.tomcat.util.http.fileupload.disk.DiskFileItemFactory;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;
import org.apache.tomcat.util.http.fileupload.servlet.ServletRequestContext;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
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
    }

    @Override
    public void run() {
        processRequest();
    }

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

        WebAppServiceExecutor executor = new WebAppServiceExecutor(requestContext);
        executor.startup();
    }

    // 採用檔案處理方式解析 form-data 資料內容
    private void parseFormData() {
        // 上傳設定初始化
        File tempFile = new File(WebAppSettingBuilder.build().getPathContext().getTempDirPath());
        DiskFileItemFactory dfac = new DiskFileItemFactory();
        dfac.setSizeThreshold(4096);
        dfac.setRepository(tempFile);
        ServletFileUpload upload = new ServletFileUpload(dfac);

        // 直到碰到下一個檔案處理才重置 progress 訊息內容
        requestContext.resetUploadProgress();

        // 上傳進度監聽處理
        // upload.setProgressListener(this::updateUploadProgressToSession);

        // 上傳表單列表內容處理
        List<FileItem> items;
        try {
            items = upload.parseRequest(new ServletRequestContext((HttpServletRequest) asyncContext.getRequest()));
            createFileTable(items);
        } catch(Exception e) {
            e.printStackTrace();

            // 上傳檔案過程發生例外錯誤時
            try {
                asyncContext.complete();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    // TODO 此處實作更新上傳進度至 Session 資料中，要注意此處為非執行緒安全操作
    /*
    private void updateUploadProgressToSession(long readByte, long maxByte, int itemIndex) {
        HttpSession session = ((HttpServletRequest) asyncContext.getRequest()).getSession();
        double dTmp = (double) readByte / (double) maxByte;
        int percent = Double.valueOf(dTmp * 100).intValue();
        if(percent > 100) percent = 100;
    }
    */

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
            res = buf.toString("UTF-8");
            buf.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    // URL 傳值處理
    private void parseParams() {
        HashMap<String, String> params = new HashMap<>();
        for (Map.Entry<String, String[]> entry : asyncContext.getRequest().getParameterMap().entrySet()) {
            String key = entry.getKey();
            String[] values = entry.getValue();
            if(values.length > 1) {
                // 單一 key 具有多個參數時
                for(int i = 0, len = values.length; i < len; i++) {
                    if(i == 0) {
                        params.put(key, values[i]);
                    } else {
                        params.put(key+"_"+i, values[i]);
                    }
                }
            } else {
                // 單個參數時
                params.put(key, values[0]);
            }
        }
        webAppStartup(params, null);
    }

}
