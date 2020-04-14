package framework.web.runnable;

import com.alibaba.fastjson.JSONArray;
import framework.observer.Handler;
import framework.observer.Message;
import framework.web.context.AsyncActionContext;
import framework.web.executor.WebAppServiceExecutor;
import framework.web.multipart.FileItem;
import framework.web.multipart.FileItemList;
import framework.web.listener.AsyncReadListener;
import framework.web.session.context.UserContext;
import framework.web.session.pattern.UserMap;
import framework.web.session.service.SessionServiceStatic;

import javax.servlet.AsyncContext;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.lang.ref.WeakReference;
import java.util.*;

/**
 * 請求封裝層類別關係：
 * WebAppController > AsyncContextRunnable
 * 此類別封裝 HttpRequest 於 AsyncActionContext 之中，
 * 進入 RequestHandler 中將以 AsyncActionContext 型態處理請求內容為主。
 */
public class AsyncContextRunnable implements Runnable {

    private ServletContext servletContext;
    // private ServletConfig servletConfig;
    private AsyncContext asyncContext;

    private AsyncActionContext requestContext;

    /**
     * 每個非同步請求實例派發給個別的 AsyncContextRunnable 隔離執行
     */
    private AsyncContextRunnable(ServletContext servletContext, ServletConfig servletConfig, AsyncContext asyncContext) {
        this.servletContext = servletContext;
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
    // jQuery 文件上传进度提示：https://segmentfault.com/a/1190000008791342
    private void parseFormData() {
        // TODO [stable-testing] AsyncReadListener
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
