package framework.web.servlet;

import framework.file.FileFinder;
import framework.thread.ThreadPoolStatic;
import framework.web.runnable.AsyncContextRunnable;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.File;
import java.lang.ref.WeakReference;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;

/**
 * 使用本套件後所有 HTTP 服務進入點為此處
 */
public class WebAppController extends HttpServlet {

    @Override
    public void init() throws ServletException {
        super.init();
        // 刪除既有暫存檔
        {
            File app_dir = new FileFinder.Builder().build().find("WEB-INF").getParentFile();
            String app_name = app_dir.getName();
            File app_temp_dir = new FileFinder.Builder().build().find("WebAppFiles");
            String dir_slash = System.getProperty("file.separator");
            Path temp_file_dir = Paths.get(app_temp_dir + dir_slash + app_name + dir_slash + "temp");
            if (temp_file_dir.toFile().exists()) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(temp_file_dir)) {
                    for (Path _path : stream) {
                        _path.toFile().delete();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

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
                    throw new Exception("getServletContext() is null");
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
