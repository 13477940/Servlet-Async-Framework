package framework.web.runnable;

import com.github.elopteryx.upload.PartOutput;
import com.github.elopteryx.upload.UploadParser;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import framework.observer.Handler;
import framework.observer.Message;
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
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.ref.WeakReference;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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

    // proc request by Content-Type header value
    private void processRequest() {
        String content_type = asyncContext.getRequest().getContentType();
        // from browser
        if(null == content_type || content_type.length() == 0) {
            parseParams();
            return;
        }
        String _content_type = content_type.toLowerCase();
        // structured http request
        if(_content_type.contains("application/x-www-form-urlencoded")) {
            proc_url_encoded_body();
            return;
        }
        // structured http request
        if(_content_type.contains("multipart/form-data")) {
            parseFormData();
            return;
        }
        // for GraphQL, JSON
        if(_content_type.contains("application/json")) {
            parseParams();
            return;
        }
        // for XML
        if(_content_type.contains("application/xml")) {
            parseParams();
            return;
        }
        // for YAML
        if(_content_type.contains("text/yaml")) {
            parseParams();
            return;
        }
        // for EDN
        if(_content_type.contains("application/edn")) {
            parseParams();
            return;
        }
        // when unstructured http request return 'error 400 bad request'
        {
            response400(new Handler(){
                @Override
                public void handleMessage(Message m) {
                    super.handleMessage(m);
                    requestContext.complete();
                }
            });
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
    // 由 Session 處理上傳進度值會影響伺服器效率，僅建議由前端處理上傳進度即可
    // 前端 AJAX 操作推薦採用 https://github.com/axios/axios
    private void parseFormData() {
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
            {
                for ( Map.Entry<String, String[]> entry : asyncContext.getRequest().getParameterMap().entrySet() ) {
                    try {
                        String key = entry.getKey();
                        String[] values = entry.getValue();
                        // if single key has more than one value, they will be add in JSONArray String.
                        if (values.length > 1) {
                            JsonArray arr = new JsonArray();
                            // Collections.addAll(arr, values);
                            for(String str : values) {
                                arr.add(str);
                            }
                            params.put(key, new Gson().toJson(arr));
                        } else {
                            String value = values[0];
                            params.put(key, value);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            final HashMap<String, File> fileTmp = new HashMap<>();
            fileTmp.put("file", null);
            int bMaxSize = 1024 * 16; // buffer size limit
            try {
                UploadParser uploadParser = new WeakReference<>( UploadParser.newParser() ).get();
                {
                    assert uploadParser != null;
                    uploadParser
                        // https://github.com/Elopteryx/upload-parser/blob/master/upload-parser-core/src/main/java/com/github/elopteryx/upload/OnPartBegin.java
                        .onPartBegin((context, buffer) -> {
                            // multipart 內容檔案化
                            String prefixName = "upload_temp_" + System.currentTimeMillis() + "_";
                            File nowFile = new WeakReference<>( File.createTempFile(prefixName, null, _targetFile) ).get();
                            {
                                assert nowFile != null;
                                nowFile.deleteOnExit(); // when close webapp
                                fileTmp.put("file", nowFile);
                            }
                            // 建立檔案輸出 OutputStream
                            BufferedOutputStream bOut;
                            {
                                bOut = new WeakReference<>( new BufferedOutputStream(new FileOutputStream(nowFile)) ).get();
                                assert bOut != null;
                                bOut.write(buffer.array());
                            }
                            return PartOutput.from(bOut);
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
                        .sizeThreshold( 0 ) // buffer 起始值（建議為 0，要不然會有 buffer 內容重複的問題）
                        .maxBytesUsed( bMaxSize ) // max buffer size
                        .maxPartSize( Long.MAX_VALUE ) // 單個 form-data 項目大小限制
                        .maxRequestSize( Long.MAX_VALUE ) // 整體 request 大小限制
                        .setupAsyncParse( (HttpServletRequest) asyncContext.getRequest() );
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // 處理 HttpRequest Parameters
    private void parseParams() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        for ( Map.Entry<String, String[]> entry : asyncContext.getRequest().getParameterMap().entrySet() ) {
            try {
                String key = entry.getKey();
                String[] values = entry.getValue();
                // if single key has more than one value, they will be add in JSONArray String.
                if (values.length > 1) {
                    JsonArray arr = new JsonArray();
                    for(String str : values) {
                        arr.add(str);
                    }
                    params.put(key, new Gson().toJson(arr));
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

    // proc application/x-www-form-urlencoded
    private void proc_url_encoded_body() {
        String param_str = requestContext.getRequestTextContent();
        String mode = "key"; // 'key' or 'value' mode, default is key
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        {
            for ( Map.Entry<String, String[]> entry : asyncContext.getRequest().getParameterMap().entrySet() ) {
                try {
                    String key = entry.getKey();
                    String[] values = entry.getValue();
                    // if single key has more than one value, they will be add in JSONArray String.
                    if (values.length > 1) {
                        JsonArray arr = new JsonArray();
                        // Collections.addAll(arr, values);
                        for(String str : values) {
                            arr.add(str);
                        }
                        params.put(key, new Gson().toJson(arr));
                    } else {
                        String value = values[0];
                        params.put(key, value);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if(null == param_str || param_str.length() == 0) {
            webAppStartup(params, null);
            return;
        }
        // proc request content value
        String now_key = null;
        {
            StringBuilder sbd = new StringBuilder(); // char pool
            for(int i = 0, len = param_str.length(); i < len; i++) {
                String str = String.valueOf(param_str.charAt(i)); // by word
                switch (str) {
                    // value
                    case "=": {
                        // if duplicate
                        if("value".equals(mode)) continue;
                        // key is over
                        {
                            now_key = URLDecoder.decode(sbd.toString(), StandardCharsets.UTF_8);
                            params.put(now_key, "");
                        }
                        // next
                        mode = "value";
                        sbd.delete(0, sbd.length());
                    } break;
                    // key
                    case "&": {
                        // if duplicate
                        if("key".equals(mode)) continue;
                        // value is over
                        {
                            params.put(now_key, URLDecoder.decode(sbd.toString(), StandardCharsets.UTF_8));
                        }
                        // next
                        mode = "key";
                        sbd.delete(0, sbd.length());
                    } break;
                    default: {
                        switch (mode) {
                            case "key":
                            case "value": {
                                sbd.append(str);
                            } break;
                        }
                    } break;
                }
            }
            // flush
            {
                switch (mode) {
                    case "key": {
                        now_key = URLDecoder.decode(sbd.toString(), StandardCharsets.UTF_8);
                        params.put(now_key, "");
                    } break;
                    case "value": {
                        params.put(now_key, URLDecoder.decode(sbd.toString(), StandardCharsets.UTF_8));
                    } break;
                }
            }
        }
        webAppStartup(params, null);
    }

    // proc application/octet-stream - single file byte
    private void proc_octet_stream() {
        File file = requestContext.getRequestByteContent();
        FileItemList fileItemList = new FileItemList();
        if(null == file) {
            try {
                throw new Exception("不是有效的二進位檔案內容");
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
        String file_content_type = "application/octet-stream";
        {
            try {
                file_content_type = Files.probeContentType(file.toPath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // add to FileItemList
        {
            FileItem fileItem = new FileItem.Builder()
                    .setFile(file)
                    .setContentType(file_content_type)
                    .setName(file.getName())
                    .setSize(file.length())
                    .setIsFormField(false)
                    .setFieldName(file.getName())
                    .build();
            fileItemList.add(fileItem);
        }
        webAppStartup(null, fileItemList);
    }

    // return bad request
    private void response400(Handler handler) {
        try {
            requestContext.getHttpResponse().sendError(HttpServletResponse.SC_BAD_REQUEST);
            if(null != handler) {
                handler.obtainMessage().sendToTarget();
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
