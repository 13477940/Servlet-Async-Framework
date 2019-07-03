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
import java.util.concurrent.ExecutorService;

public class WebAppController extends HttpServlet {

    private ExecutorService worker = null; // 執行緒池

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
                worker = ThreadPoolStatic.getInstance();
            }
        }
        setEncoding(req, resp);
        AsyncContext asyncContext;
        // Start Async 檢查是否已具有非同步狀態
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
                    // 此 Framework 無法支援同步請求，所以將直接不處理 request
                    return;
                }
            }
        }
        {
            // 上傳和下載持續時間皆會受到 AsyncContext Timeout 的影響，所以要依照請求處理內容去定義比較適合
            // 設置為 0 時表示非同步處理中無逾時限制(ms)
            asyncContext.setTimeout(0);
        }
        {
            if(null == ServletContextStatic.InstanceHolder.instance) {
                ServletContextStatic.InstanceHolder.instance = new WeakReference<>(getServletContext()).get();
            }
        }
        AsyncContextRunnable asyncContextRunnable = new AsyncContextRunnable.Builder()
                .setServletContext(new WeakReference<>(getServletContext()).get())
                .setServletConfig(new WeakReference<>(getServletConfig()).get())
                .setAsyncContext(new WeakReference<>(asyncContext).get())
                .build();
        worker.execute(asyncContextRunnable);
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
        ThreadPoolStatic.shutdown();
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
