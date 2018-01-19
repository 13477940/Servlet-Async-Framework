package framework.web.servlet;

import framework.web.http.OkHttpClientBuilder;
import framework.web.runnable.AsyncContextRunnable;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WebAppController extends HttpServlet {

    private ExecutorService worker = null; // 執行緒池

    public WebAppController() { worker = Executors.newCachedThreadPool(); }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        startAsync(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        startAsync(req, resp);
    }

    // 執行非同步 Servlet 服務
    private void startAsync(HttpServletRequest req, HttpServletResponse resp) {
        setEncoding(req, resp);
        boolean canAsync = false;
        AsyncContext asyncContext = null;
        // 是否已具有非同步狀態
        if(req.isAsyncStarted()) {
            asyncContext = req.getAsyncContext();
            canAsync = true;
        } else {
            if(req.isAsyncSupported()) { // 是否支援非同步架構
                asyncContext = req.startAsync();
                canAsync = true;
            } else {
                System.err.println("Servlet 或 Fileter 等尚未全部開啟非同步支援(async-supported)");
            }
        }
        // 藉由執行緒池接手後續任務，確保 Tomcat 端執行緒可以持續的接收請求
        // 並且藉由個別的 Runnable 隔離每個 asyncContext 處理狀態
        if(canAsync) {
            asyncContext.setTimeout(0); // 設置為 0 時表示非同步處理中無逾時限制
            worker.submit(new AsyncContextRunnable(asyncContext));
        }
    }

    // 內容編碼設定
    private void setEncoding(HttpServletRequest req, HttpServletResponse resp) {
        String encoding = "UTF-8";
        try {
            req.setCharacterEncoding(encoding);
            resp.setCharacterEncoding(encoding);
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
                    if (!worker.awaitTermination(3000, TimeUnit.MILLISECONDS)) {
                        // 當回收時限到期時，強制中斷所有 Thread 執行
                        worker.shutdownNow();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // 回收時發生錯誤時亦強制關閉所有 thread
                    worker.shutdownNow();
                }
            }
        }
        {
            OkHttpClientBuilder.shutdown();
        }
        {
            // SimpleDataSource.shutdown();
            // TomcatDataSource.shutdown();
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
