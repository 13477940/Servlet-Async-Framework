package framework.servlet;

import framework.runnable.AsyncContextRunnable;

import javax.servlet.AsyncContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebAppController extends HttpServlet {

    private ExecutorService worker; // 執行緒池

    public WebAppController() {
        worker = Executors.newWorkStealingPool();
    }

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
                System.err.println("Servlet 或 Fileter 等尚未全部開啟非同步支援");
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

    @Override
    public void destroy() {
        super.destroy();
        worker.shutdown(); // 回收執行緒池
    }

}
