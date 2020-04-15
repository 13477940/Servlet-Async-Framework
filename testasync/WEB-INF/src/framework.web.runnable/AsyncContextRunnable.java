package framework.web.runnable;

import com.alibaba.fastjson.JSONArray;
import com.github.elopteryx.upload.PartOutput;
import com.github.elopteryx.upload.UploadParser;
import framework.random.RandomServiceStatic;
import framework.setting.AppSetting;
import framework.web.context.AsyncActionContext;
import framework.web.executor.WebAppServiceExecutor;
import framework.web.multipart.FileItem;
import framework.web.multipart.FileItemList;
import framework.web.session.context.UserContext;
import framework.web.session.pattern.UserMap;
import framework.web.session.service.SessionServiceStatic;

import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.util.*;

/**
 * 請求封裝層類別關係：
 * WebAppController > AsyncContextRunnable
 * 此類別封裝 HttpRequest 於 AsyncActionContext 之中，
 * 進入 RequestHandler 中將以 AsyncActionContext 型態處理請求內容為主。
 */
public class AsyncContextRunnable implements Runnable {

    // private final ServletContext servletContext;
    // private ServletConfig servletConfig;
    private final AsyncContext asyncContext;
    private final AsyncActionContext requestContext;

    /**
     * 每個非同步請求實例派發給個別的 AsyncContextRunnable 隔離執行
     */
    private AsyncContextRunnable(ServletContext servletContext, ServletConfig servletConfig, AsyncContext asyncContext) {
        // this.servletContext = servletContext;
        // this.servletConfig = servletConfig;
        this.asyncContext = asyncContext;
        {
            AsyncActionContext.Builder contextBuilder = new AsyncActionContext.Builder();
            {
                contextBuilder.setServletContext(servletContext);
                contextBuilder.setServletConfig(servletConfig);
                contextBuilder.setAsyncContext(asyncContext);
            }
            this.requestContext = new WeakReference<>(contextBuilder.build()).get();
            assert null != this.requestContext;
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

    private void webAppStartup(LinkedHashMap<String, String> params, FileItemList files) {
        // HTTP Header 處理
        {
            HttpServletRequest req = (HttpServletRequest) asyncContext.getRequest();
            ArrayList<String> names = Collections.list(req.getHeaderNames());
            HashMap<String, String> headers = new HashMap<>();
            for(String key : names) {
                String value = req.getHeader(key);
                headers.put(key, value);
            }
            requestContext.setHeaders(headers);
        }
        // 處理參數內容及上傳檔案
        {
            requestContext.setParameters(params);
            if (null == files || files.size() == 0) {
                requestContext.setIsFileAction(false); // 不具有檔案上傳請求
                requestContext.setFiles(null);
            } else {
                requestContext.setIsFileAction(true); // 具有檔案上傳請求
                requestContext.setFiles(files);
            }
        }
        // 每個 WebAppServiceExecutor 獨立處理完一個 AsyncActionContext 的 Task 內容
        WebAppServiceExecutor executor = new WebAppServiceExecutor(requestContext);
        executor.startup();
    }

    // 採用檔案處理方式解析 multipart/form-data 資料內容
    // 由於 Session 處理上傳進度值會影響伺服器效率，僅建議由前端處理上傳進度即可
    // 前端 AJAX 操作推薦採用 https://github.com/axios/axios
    private void parseFormData() {
        /*
        Handler asyncReadHandler = new Handler() {
            @Override
            public void handleMessage(Message m) {
                super.handleMessage(m);
                {
                    FileItemList files = null;
                    LinkedHashMap<String, String> params = new LinkedHashMap<>();
                    {
                        String key = m.getData().getString("key");
                        if(null != m.getData().get(key)) {
                            FileItemList fileItems = (FileItemList) m.getData().get(key);
                            for (FileItem item : fileItems.prototype()) {
                                if (item.isFormField()) {
                                    params.put(item.getFieldName(), item.getContent());
                                } else {
                                    if(null == files) files = new FileItemList();
                                    files.add(item);
                                }
                            }
                        }
                    }
                    // upload done
                    if(null == files || files.size() == 0) {
                        webAppStartup(params, null);
                    } else {
                        webAppStartup(params, files);
                    }
                }
            }
        };
        AsyncReadListener asyncReadListener = new AsyncReadListener.Builder()
                .setServletContext(servletContext)
                .setAsyncContext(asyncContext)
                .setHandler(asyncReadHandler)
                .build();
        try {
            asyncContext.getRequest().getInputStream().setReadListener(asyncReadListener);
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        // Elopteryx/upload-parser
        // https://github.com/Elopteryx/upload-parser
        // 使用該模組解決原本 byte to byte 的無 Buffer 效率問題
        {
            File targetFile = null;
            AppSetting appSetting = new AppSetting.Builder().build();
            try {
                targetFile = new File(appSetting.getPathContext().getTempDirPath());
            } catch (Exception e) {
                e.printStackTrace();
            }
            final File _targetFile = targetFile;
            FileItemList fileItemList = new FileItemList();
            LinkedHashMap<String, String> params = new LinkedHashMap<>();
            final HashMap<String, File> fileTmp = new HashMap<>();
            fileTmp.put("file", null);
            try {
                UploadParser.newParser()
                        .onPartBegin((context, byteBuffer) -> {
                            File nowFile = new WeakReference<>( File.createTempFile(RandomServiceStatic.getInstance().getTimeHash(12), null, _targetFile) ).get();
                            assert nowFile != null;
                            nowFile.deleteOnExit();
                            fileTmp.put("file", nowFile);
                            // 快取清零，因小資料塞不滿 byteBuffer 會造成一堆 byte 0
                            // https://stackoverflow.com/questions/17003164/byte-array-with-padding-of-null-bytes-at-the-end-how-to-efficiently-copy-to-sma
                            byte[] byteArr = byteBuffer.array();
                            byte[] fixByteArr;
                            {
                                int i = byteArr.length - 1;
                                while ( i >= 0 && byteArr[i] == 0 ) --i;
                                fixByteArr = Arrays.copyOf(byteArr, i + 1);
                            }
                            // 開頭資料會先儲存在 byteBuffer，所以要先處理完 byteBuffer 內的資料才能確保檔案完整
                            try (BufferedOutputStream bOut = new BufferedOutputStream(new FileOutputStream(nowFile))) {
                                bOut.write(fixByteArr);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                            return PartOutput.from(nowFile.toPath());
                        })
                        .onPartEnd((context) -> {
                            String field_name = context.getCurrentPart().getName();
                            if(context.getCurrentPart().isFile()) {
                                FileItem fileItem = new FileItem.Builder()
                                        .setFile(fileTmp.get("file"))
                                        .setContentType(context.getCurrentPart().getContentType())
                                        .setName(context.getCurrentPart().getSubmittedFileName())
                                        .setSize(context.getCurrentPart().getKnownSize())
                                        .setIsFormField(false)
                                        .setFieldName(context.getCurrentPart().getName())
                                        .build();
                                fileItemList.add(fileItem);
                            } else {
                                File nowFile = fileTmp.get("file");
                                String param_value = Files.readString(nowFile.toPath());
                                params.put(field_name, param_value.trim());
                            }
                        })
                        .onRequestComplete(context -> {
                            fileTmp.clear();
                            {
                                if(fileItemList.size() == 0) {
                                    webAppStartup(params, null);
                                } else {
                                    webAppStartup(params, fileItemList);
                                }
                            }
                        })
                        .onError((context, throwable) -> throwable.printStackTrace())
                        .sizeThreshold( 1024 * 4 ) // buffer size
                        .maxPartSize( Long.MAX_VALUE ) // 單個 form-data 項目大小限制
                        .maxRequestSize( Long.MAX_VALUE ) // 整體 request 大小限制
                        .setupAsyncParse( (HttpServletRequest) asyncContext.getRequest() );
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 處理 HttpRequest Parameters（使用 HashMap 因為參數通常對順序不敏感）
    private void parseParams() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
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
