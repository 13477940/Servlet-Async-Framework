package framework.web.runnable;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import framework.logs.LoggerService;
import framework.observer.Handler;
import framework.observer.Message;
import framework.web.context.AsyncActionContext;
import framework.web.executor.WebAppServiceExecutor;
import framework.web.listener.AsyncReadListener;
import framework.web.multipart.FileItem;
import framework.web.multipart.FileItemList;
import framework.web.session.context.UserContext;
import framework.web.session.pattern.UserMap;
import framework.web.session.service.SessionServiceStatic;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.tika.Tika;
import org.apache.tomcat.util.http.fileupload.ParameterParser;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.lang.ref.WeakReference;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    // process request by content type
    private void processRequest() {
        String content_type = null;
        if(null != asyncContext.getRequest().getContentType()) {
            content_type = asyncContext.getRequest().getContentType().toLowerCase(Locale.ENGLISH);
        }
        // from browser
        if(null == content_type || content_type.isEmpty()) {
            LinkedHashMap<String, String> params = parse_params();
            webAppStartup(params, null);
            return;
        }
        String _content_type = content_type.toLowerCase(Locale.ENGLISH);
        // structured http request
        if(_content_type.contains("application/x-www-form-urlencoded")) {
            LinkedHashMap<String, String> params = parse_url_encoded_body();
            webAppStartup(params, null);
            return;
        }
        // structured http request
        if(_content_type.contains("multipart/form-data")) {
            parse_multipart_form();
            return;
        }
        if(_content_type.contains("application/octet-stream")) {
            parse_binary_req();
            return;
        }
        // for GraphQL, JSON
        if(_content_type.contains("application/json")) {
            LinkedHashMap<String, String> params = parse_params();
            webAppStartup(params, null);
            return;
        }
        // for XML
        if(_content_type.contains("application/xml")) {
            LinkedHashMap<String, String> params = parse_params();
            webAppStartup(params, null);
            return;
        }
        // for YAML
        if(_content_type.contains("text/yaml")) {
            LinkedHashMap<String, String> params = parse_params();
            webAppStartup(params, null);
            return;
        }
        // for EDN
        if(_content_type.contains("application/edn")) {
            LinkedHashMap<String, String> params = parse_params();
            webAppStartup(params, null);
            return;
        }
        // for text/plain
        if(_content_type.contains("text/plain")) {
            LinkedHashMap<String, String> params = parse_params();
            webAppStartup(params, null);
            return;
        }
        // when unstructured http request return 'error 400 bad request'
        {
            String msg_zht = "未支援的請求格式："+content_type;
            LoggerService.logERROR(msg_zht);
            System.err.println(msg_zht);
            response400(new Handler(){
                @Override
                public void handleMessage(Message m) {
                    super.handleMessage(m);
                    if(null != requestContext) {
                        requestContext.complete();
                    }
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
            if (null == files || files.isEmpty()) {
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

    /**
     * 採用檔案處理方式解析 multipart/form-data 資料內容
     * 因為由 Session 處理上傳進度值會影響伺服器效率，僅建議由前端處理上傳進度監聽即可
     * 前端 AJAX 操作推薦採用 <a href="https://github.com/axios/axios">axios</a>
     */
    private void parse_multipart_form() {
        LinkedHashMap<String, String> params = parse_params();
        // 透過自定義的 AsyncReadListener 處理 MultiPart
        try {
            FileItemList fileItemList = new FileItemList();
            // get boundary string
            ParameterParser parameterParser = new ParameterParser();
            parameterParser.setLowerCaseNames(true);
            Map<String, String> req_content_type = parameterParser.parse(requestContext.getHttpRequest().getContentType(), new char[] {';', ','});
            String boundary_string = req_content_type.get("boundary");

            ServletInputStream servletInputStream = requestContext.getHttpRequest().getInputStream();
            servletInputStream.setReadListener(new AsyncReadListener(servletInputStream, boundary_string, new Handler(){
                @Override
                public void handleMessage(Message m) {
                    super.handleMessage(m);
                    String status = m.getData().getString("status");
                    if("done".equalsIgnoreCase(status)) {
                        JsonArray data_arr = new Gson().fromJson(m.getData().getString("data"), JsonArray.class);
                        for(int i = 0, len = data_arr.size(); i < len; i++) {
                            JsonObject obj = data_arr.get(i).getAsJsonObject();
                            if(obj.has("filename")) {
                                Path file_path = Paths.get(obj.get("file_path").getAsString());
                                FileItem fileItem = new FileItem.Builder()
                                        .setFile(file_path.toFile())
                                        .setName(obj.get("filename").getAsString())
                                        .setContentType(obj.get("content-type").getAsString())
                                        .setFieldName(obj.get("name").getAsString())
                                        .setSize(file_path.toFile().length())
                                        .setIsFormField(false)
                                        .build();
                                fileItemList.add(fileItem);
                            } else {
                                Path file_path = Paths.get(obj.get("file_path").getAsString());
                                try ( FileInputStream fis = new FileInputStream(file_path.toString()) ) {
                                    String key = obj.get("name").getAsString();
                                    String value = new String(fis.readAllBytes(), StandardCharsets.UTF_8);
                                    params.put(key, value);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        if(fileItemList.isEmpty()) {
                            webAppStartup(params, null);
                        } else {
                            webAppStartup(params, fileItemList);
                        }
                    }
                }
            }));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getRequestTextContent() {
        String str = null;
        try {
            BufferedInputStream bis = new BufferedInputStream(asyncContext.getRequest().getInputStream());
            str = new String(bis.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return str;
    }

    // parse application/x-www-form-urlencoded
    private LinkedHashMap<String, String> parse_url_encoded_body() {
        LinkedHashMap<String, String> params = parse_params();
        String param_str = getRequestTextContent();
        if(null == param_str || param_str.isEmpty()) {
            return params;
        }
        Arrays.stream(param_str.split("&"))
            .map(s -> s.split("=", 2))
            .forEach(kv -> params.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
            URLDecoder.decode(kv[1], StandardCharsets.UTF_8)));
        return params;
    }

    // 處理 HttpRequest Parameters
    private LinkedHashMap<String, String> parse_params() {
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        for ( Map.Entry<String, String[]> entry : asyncContext.getRequest().getParameterMap().entrySet() ) {
            try {
                String key = entry.getKey();
                String[] values = entry.getValue();
                // if single key has more than one value, they will add in JSONArray String.
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
        return params;
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

    // parse application/octet-stream - single file byte
    private void parse_binary_req() {
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
                file_content_type = new Tika().detect(file);
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
                    .setFieldName(file.getName())
                    .setIsFormField(false)
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
