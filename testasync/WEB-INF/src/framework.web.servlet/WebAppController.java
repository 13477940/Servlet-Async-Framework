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

/**
 * ＃2021-05-05 fixed for tomcat 10 to jakarta
 */
public class WebAppController extends HttpServlet {

    // 以 service() 處理請求，自行決定各 http method 處理方式
    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) {
        startAsync(req, resp);
    }

    // 當 Servlet 被回收時，關閉常駐資源
    @Override
    public void destroy() {
        super.destroy();
        ThreadPoolStatic.shutdown();
        deregisterDrivers();
    }

    private void startAsync(HttpServletRequest req, HttpServletResponse resp) {
        setEncoding(req, resp);
        // https://docs.oracle.com/javaee/6/api/javax/servlet/AsyncContext.html
        AsyncContext asyncContext;
        if(req.isAsyncStarted()) {
            asyncContext = req.getAsyncContext();
        } else {
            if(req.isAsyncSupported()) {
                asyncContext = req.startAsync();
            } else {
                try {
                    throw new Exception("Servlet 或 Filter 等類別尚未全部開啟非同步支援(async-supported)");
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return;
            }
        }
        // Async Request Timeout
        {
            // Sets the timeout (in milliseconds) for this AsyncContext.
            // 上傳和下載持續時間皆會受到 AsyncContext Timeout 的影響
            // 設置為 0 時表示非同步處理中無逾時限制(ms)
            asyncContext.setTimeout(0);
        }
        // ServletContext
        // https://openhome.cc/Gossip/ServletJSP/ServletContext.html
        {
            if(null == getServletContext()) {
                try {
                    StringBuilder sbd = new StringBuilder();
                    {
                        sbd.append("目前無法正常執行 Servlet 任務，");
                        sbd.append("請檢查 getServletContext() 為空值的原因，");
                        sbd.append("通常是 thread pool 有未正常結束的任務卡住佇列");
                    }
                    throw new Exception(sbd.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            ServletContextStatic.setInstance(getServletContext());
        }
        // Process Async Request
        AsyncContextRunnable asyncContextRunnable = new AsyncContextRunnable.Builder()
                .setServletContext( new WeakReference<>( getServletContext() ).get() )
                .setServletConfig( new WeakReference<>( getServletConfig() ).get() )
                .setAsyncContext( new WeakReference<>( asyncContext ).get() )
                .build();
        /*
         * 目前測出
         * 『自定義 ThreadPool』、
         * 『asyncContext.getResponse().getBufferSize()』
         * 會造成 AppSetting 發生錯誤
         */
        ThreadPoolStatic.execute(asyncContextRunnable);
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
    private void deregisterDrivers() {
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

}
