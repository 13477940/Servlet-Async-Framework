package framework.web.servlet;

import framework.thread.ThreadPoolStatic;
import framework.web.runnable.AsyncContextRunnable;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

public class WebAppController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        startAsync(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        startAsync(req, resp);
    }

    private void startAsync(HttpServletRequest req, HttpServletResponse resp) {
        setEncoding(req, resp);
        AsyncContext asyncContext;
        if(req.isAsyncStarted()) {
            asyncContext = req.getAsyncContext();
        } else {
            if(req.isAsyncSupported()) {
                asyncContext = req.startAsync();
            } else {
                try {
                    throw new Exception("Servlet 或 Fileter 等類別尚未全部開啟非同步支援(async-supported)");
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
        }
        // Async Request Timeout
        {
            // 上傳和下載持續時間皆會受到 AsyncContext Timeout 的影響
            // 設置為 0 時表示非同步處理中無逾時限制(ms)
            asyncContext.setTimeout(0);
        }
        // Servlet Context
        {
            if(null == ServletContextStatic.getInstance()) {
                ServletContextStatic.setInstance( new WeakReference<>( getServletContext() ).get() );
            }
        }
        // Process Async Request
        AsyncContextRunnable asyncContextRunnable = new AsyncContextRunnable.Builder()
                .setServletContext( new WeakReference<>( getServletContext() ).get() )
                .setServletConfig( new WeakReference<>( getServletConfig() ).get() )
                .setAsyncContext( new WeakReference<>( asyncContext ).get() )
                .build();
        ThreadPoolStatic.getInstance().execute(asyncContextRunnable);
    }

    // 內容編碼設定
    private void setEncoding(HttpServletRequest req, HttpServletResponse resp) {
        String charset = StandardCharsets.UTF_8.name();
        try {
            req.setCharacterEncoding(charset);
            resp.setCharacterEncoding(charset);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 釋放 WebApp 用到的 Driver Class 資源
    private void unRegAppDrivers() {
        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while(drivers.hasMoreElements()) {
            Driver driver = drivers.nextElement();
            try {
                DriverManager.deregisterDriver(driver);
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }
    }

    // 當 Servlet 被回收時，關閉常駐資源
    @Override
    public void destroy() {
        super.destroy();
        ThreadPoolStatic.shutdown();
        unRegAppDrivers();
    }

}
