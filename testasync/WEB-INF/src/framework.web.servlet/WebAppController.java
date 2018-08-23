package framework.web.servlet;

import framework.web.http.OkHttpClientStatic;
import framework.web.runnable.AsyncContextRunnable;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WebAppController extends HttpServlet {

    private ExecutorService worker = null; // 執行緒池

    public WebAppController() {}

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        startAsync(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        startAsync(req, resp);
    }

    // 執行非同步 Servlet 服務
    private void startAsync(HttpServletRequest req, HttpServletResponse resp) {
        {
            // 直到被進行實例化後呼叫功能才建立執行緒池
            if(null == worker || worker.isTerminated() || worker.isShutdown()) {
                worker = Executors.newCachedThreadPool();
            }
        }
        setEncoding(req, resp);
        AsyncContext asyncContext;
        // 是否已具有非同步狀態
        if(req.isAsyncStarted()) {
            asyncContext = req.getAsyncContext();
        } else {
            if(req.isAsyncSupported()) { // 是否支援非同步架構
                asyncContext = req.startAsync();
            } else {
                try {
                    throw new Exception("Servlet 或 Fileter 等尚未全部開啟非同步支援(async-supported)");
                } catch (Exception e) {
                    e.printStackTrace();
                    return; // 無非同步架構下不接受 Http Request
                }
            }
        }
        // 藉由執行緒池接手後續任務，確保 Tomcat 端執行緒可以持續的接收請求
        // 並且藉由個別的 Runnable 隔離每個 asyncContext 處理狀態
        asyncContext.setTimeout(0); // 設置為 0 時表示非同步處理中無逾時限制
        worker.submit(new AsyncContextRunnable(asyncContext));
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

    /**
     * 由於 Servlet 在 WebApp 中屬於單一實例多執行緒，
     * 所以當 Servlet 被回收時，亦代表整個 WebApp 即將關閉
     * 可由此處封裝不需經過使用者操作的資源回收指令
     */
    @Override
    public void destroy() {
        super.destroy();
        {
            // 實作完整回收 ExecutorService 方式
            // http://blog.csdn.net/xueyepiaoling/article/details/61200270
            if (null != worker && !worker.isShutdown()) {
                // 設定 worker 已不能再接收新的請求
                worker.shutdown();
                try {
                    // 設定一個 await 時限提供 thread 完成未完畢的工作的最後期限
                    if (!worker.awaitTermination(3, TimeUnit.SECONDS)) {
                        // 當回收時限到期時，強制中斷所有 Thread 執行
                        worker.shutdownNow();
                    }
                } catch (Exception e) {
                    // e.printStackTrace();
                    // 回收時發生錯誤時亦強制關閉所有 thread
                    worker.shutdownNow();
                }
            }
        }
        {
            OkHttpClientStatic.shutdown();
        }
        unRegAppDrivers();
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

}
